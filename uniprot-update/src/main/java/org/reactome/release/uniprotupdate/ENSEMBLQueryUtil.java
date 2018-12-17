package org.reactome.release.uniprotupdate;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reactome.release.uniprotupdate.dataschema.UniprotData;
import org.reactome.util.ensembl.EnsemblServiceResponseProcessor;
import org.reactome.util.ensembl.EnsemblServiceResponseProcessor.EnsemblServiceResult;

class ENSEMBLQueryUtil
{
	private static final String ENSEMBL_LOOKUP_URI_PREFIX = "https://rest.ensembl.org/lookup/id/";
	private static final Logger logger = LogManager.getLogger();
	private static final Pattern validSeqRegionPatter = Pattern.compile(".* seq_region_name=\"(1|2|3|4|5|6|7|8|9|10|11|12|13|14|15|16|17|18|19|20|21|22|X|Y|MT)\" .*", Pattern.MULTILINE);
	public static boolean checkOKWithENSEMBL(String ensemblGeneID) throws URISyntaxException
	{
		boolean isOK = false;
		// need to query a URL of the form:
		// https://rest.ensembl.org/lookup/id/ENSG00000204518?content-type=text/xml
		// See the old Perl version of this: EnsEMBLUtils.pm, sub on_EnsEMBL_primary_assembly
		URI uri = new URI(ENSEMBLQueryUtil.ENSEMBL_LOOKUP_URI_PREFIX + ensemblGeneID);
		URIBuilder builder = new URIBuilder();
		builder.setHost(uri.getHost()).setPath(uri.getPath()).setScheme(uri.getScheme()).addParameter("content-type", "text/xml");
		try
		{
			HttpGet get = new HttpGet(builder.build());
			String queryResponse = queryENSEMBLForGeneID(get);
			if (queryResponse != null)
			{
				queryResponse = queryResponse.replaceAll("\n", "");
				// According to the old Perl code, seq_region_name must match any of these: 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 X Y MT
				Matcher m = ENSEMBLQueryUtil.validSeqRegionPatter.matcher(queryResponse);
				if (m.matches())
				{
					isOK = true;
				}
			}
		}
		catch (URISyntaxException e)
		{
			e.printStackTrace();
			throw new Error(e);
		}
		catch (ClientProtocolException e)
		{
			e.printStackTrace();
			throw new Error(e);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			throw new Error(e);
		}
		return isOK;
	}
	
	public static String queryENSEMBLForGeneID(HttpGet get) throws URISyntaxException, IOException
	{
		EnsemblServiceResponseProcessor responseProcessor = new EnsemblServiceResponseProcessor();
		logger.trace("URI: " + get.getURI());
		boolean done = false;
		while (!done)
		{
			try (CloseableHttpClient getClient = HttpClients.createDefault(); CloseableHttpResponse getResponse = getClient.execute(get);)
			{
				EnsemblServiceResult result = responseProcessor.processResponse(getResponse, get.getURI());
				if (!result.getWaitTime().equals(Duration.ZERO))
				{
					logger.info("Need to wait: {} seconds.", result.getWaitTime().getSeconds());
					Thread.sleep(result.getWaitTime().toMillis());
					done = false;
				}
				else
				{
					// Only record the successful responses.
					if (result.getStatus() == HttpStatus.SC_OK)
					{
						String content = result.getResult().trim();
						done = true;
						return content;
					}
					else if (result.getStatus() == HttpStatus.SC_BAD_REQUEST)
					{
						logger.trace("Got BAD_REQUEST reponse. This was the request that was sent: {}", get.toString());
						done = true;
					}
					else if (result.getResult() == null && result.isOkToRetry())
					{
						logger.trace("No result, status was {}, but it is OK to retry. Retrying...", result.getStatus());
						done = false;
					}
					else
					{
						done = true;
					}
				}
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
				throw new Error(e);
			}
		}
		// If we get here, that means there was no content!
		return null;
	}

	public static void checkGenesWithENSEMBL(List<UniprotData> uniprotData, AtomicInteger totalEnsemblGeneCount, Set<String> genesOKWithENSEMBL, String ensemblGenesFileName, String speciesName) throws IOException
	{
		final long startTimeEnsemblLookup = System.currentTimeMillis();
		// we'll write in append mode, just in case we encounter new Gene IDs that weren't in the file originally.
		try(FileWriter fileWriter = new FileWriter(ensemblGenesFileName, true))
		{
			// 8 threads (my workstation has 8 cores, parallelStream defaults to 8 threads) and we start getting told "too many requests - please wait 2 seconds". This slows
			// everything down, so we should try to send as many requests as we can without hitting the 15/second rate limit.
			// I've determined experimentally that no matter how many threads try to make requests, the best rate I can get is 10 requests per second.
			// It seems that with 5 threads, I can get 10 requests/second with almost no "please wait" responses.
			System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "5");
			
			List<String> geneBuffer = Collections.synchronizedList(new ArrayList<String>(1000));
			uniprotData.parallelStream()
						.filter(data ->  data.getEnsembleGeneIDs()!=null && data.getScientificName().equals(speciesName))
						.forEach( data -> {
				List<String> geneList = new ArrayList<>();
				geneList = data.getEnsembleGeneIDs().stream().distinct().collect(Collectors.toList());
				for(String ensemblGeneID : geneList)
				{
					try
					{
						// If the gene ID is not already in the set (could happen if you're using a pre-existing gene list).
						// We'll assume that if it a Gene ID is in the list, it's OK. This *might* not be a very good assumption for Production (unless you know the list is fresh),
						// but for testing purposes, it will probably speed things up greatly.
						if (!genesOKWithENSEMBL.contains(ensemblGeneID))
						{
							// Check if the gene is "OK" with ENSEMBL. Here, "OK" means the response matches this regexp:
							// .* seq_region_name=\"(1|2|3|4|5|6|7|8|9|10|11|12|13|14|15|16|17|18|19|20|21|22|X|Y|MT)\" .*
							if (ENSEMBLQueryUtil.checkOKWithENSEMBL(ensemblGeneID))
							{
								genesOKWithENSEMBL.add(ensemblGeneID);
								// If the buffer has > 1000 genes, write to file.
								synchronized (geneBuffer)
								{
									geneBuffer.add(ensemblGeneID);
									if (geneBuffer.size() >= 1000 )
									{
										logger.info("Dumping genes to file: {}",ensemblGenesFileName);
										geneBuffer.stream().forEach(gene -> {
											try
											{
												fileWriter.write(gene + "\n");
											}
											catch (IOException e)
											{
												e.printStackTrace();
											}
										});
										// clear the buffer.
										geneBuffer.clear();
									}
								}
							}
							int amt = totalEnsemblGeneCount.getAndIncrement();
							int size = genesOKWithENSEMBL.size();
							if (amt % 1000 == 0)
							{
								long currentTime = System.currentTimeMillis();
								// unlikely, but it happened at least once during testing.
								if (currentTime == startTimeEnsemblLookup)
								{
									currentTime += 1;
								}
								logger.info("{} genes were checked with ENSEMBL, {} were \"OK\"; query rate: {} per second", amt, size,(double)size / (double)((currentTime-startTimeEnsemblLookup)/1000.0));
							}
						}
					}
					catch (URISyntaxException e)
					{
						e.printStackTrace();
					}
				}
			});
		}
		long currentTimeEnsembl = System.currentTimeMillis();
		logger.info("{} genes were checked with ENSEMBL, {} were \"OK\". Time spent: {}", totalEnsemblGeneCount.get(), genesOKWithENSEMBL.size(), Duration.ofMillis(currentTimeEnsembl - startTimeEnsemblLookup).toString());
	}
}
