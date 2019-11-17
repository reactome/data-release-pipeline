package org.reactome.release.uniprotupdate;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.http.HttpStatus;
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

class ENSEMBLQueryUtil {
	private static final String ENSEMBL_LOOKUP_URI_PREFIX = "https://rest.ensembl.org/lookup/id/";
	private static final Logger logger = LogManager.getLogger();
	private static final Pattern validSeqRegionPattern = Pattern.compile(
		".* seq_region_name=\"(1|2|3|4|5|6|7|8|9|10|11|12|13|14|15|16|17|18|19|20|21|22|X|Y|MT)\" .*",
		Pattern.MULTILINE
	);

	public static boolean checkOKWithENSEMBL(String ensemblGeneID) {
		try {
			String ensemblGeneIdResponse = queryENSEMBLForGeneID(ensemblGeneID);
			return (validSeqRegionPattern.matcher(ensemblGeneIdResponse).matches());
		} catch (URISyntaxException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	static String queryENSEMBLForGeneID(String ensemblGeneID) throws URISyntaxException {
		HttpGet httpQuery = getHttpQueryToEnsEMBL(ensemblGeneID);
		logger.trace("URI: " + httpQuery.getURI());
		return executeQuery(httpQuery);
	}

	private static String executeQuery(HttpGet httpQuery) {
		EnsemblServiceResponseProcessor responseProcessor = new EnsemblServiceResponseProcessor();
		try (CloseableHttpClient getClient = HttpClients.createDefault();
			 CloseableHttpResponse getResponse = getClient.execute(httpQuery)) {

			EnsemblServiceResult result = responseProcessor.processResponse(getResponse, httpQuery.getURI());
			return processResult(httpQuery, result);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private static String processResult(HttpGet httpQuery, EnsemblServiceResult result) {
		if (!result.getWaitTime().equals(Duration.ZERO)) {
			logger.info("Need to wait: {} seconds.", result.getWaitTime().getSeconds());
			try {
				Thread.sleep(result.getWaitTime().toMillis());
			} catch (InterruptedException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
			return executeQuery(httpQuery);
		}

		if (result.getResult() == null && result.isOkToRetry()) {
			logger.trace(
					"No result, status was {}, but it is OK to retry. Retrying...",
					result.getStatus()
			);
			return executeQuery(httpQuery);
		}

		// Only record the successful responses.
		if (result.getStatus() == HttpStatus.SC_OK) {
			return result.getResult()
				.trim()
				.replaceAll("\n", "");
		}

		if (result.getStatus() == HttpStatus.SC_BAD_REQUEST) {
			logger.trace(
				"Got BAD_REQUEST response. This was the request that was sent: {}",
				httpQuery.toString()
			);
		}
		return "";
	}

	private static HttpGet getHttpQueryToEnsEMBL(String ensemblGeneID) throws URISyntaxException {
		// need to query a URL of the form:
		// https://rest.ensembl.org/lookup/id/ENSG00000204518?content-type=text/xml
		// See the old Perl version of this: EnsEMBLUtils.pm, sub on_EnsEMBL_primary_assembly
		URI uri = new URI(ENSEMBLQueryUtil.ENSEMBL_LOOKUP_URI_PREFIX + ensemblGeneID);
		URIBuilder builder =
			new URIBuilder()
			.setHost(uri.getHost())
			.setPath(uri.getPath())
			.setScheme(uri.getScheme())
			.addParameter("content-type", "text/xml");
		return new HttpGet(builder.build());
	}

	public static Set<String> checkGenesWithENSEMBL(List<UniprotData> uniprotData, String speciesName)
		throws IOException {
		AtomicInteger totalEnsemblGeneCount = new AtomicInteger(0);
		Set<String> genesOKWithENSEMBL = Collections.synchronizedSet(new HashSet<>());

		String ensemblGenesFileName = "ensemblGeneIDs.list";
		// If the file already exists, load it into memory, into genesOKWithENSEMBL
		if (Files.exists(Paths.get(ensemblGenesFileName))) {
			Files.readAllLines(Paths.get(ensemblGenesFileName)).parallelStream().forEach(genesOKWithENSEMBL::add);
		}
//		int startingSize = genesOKWithENSEMBL.size();

		final long startTimeEnsemblLookup = System.currentTimeMillis();
		// we'll write in append mode, just in case we encounter new Gene IDs that weren't in the file originally.
		try(FileWriter fileWriter = new FileWriter(ensemblGenesFileName, true)) {
			// 8 threads (my workstation has 8 cores, parallelStream defaults to 8 threads) and we start getting told
			// "too many requests - please wait 2 seconds". This slows everything down, so we should try to send as
			// many requests as we can without hitting the 15/second rate limit.
			// I've determined experimentally that no matter how many threads try to make requests,
			// the best rate I can get is 10 requests per second.
			// It seems that with 5 threads, I can get 10 requests/second with almost no "please wait" responses.
			System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "5");

			List<String> geneBuffer = Collections.synchronizedList(new ArrayList<>(1000));
			uniprotData.parallelStream()
			.filter(data ->  data.getEnsembleGeneIDs()!=null && data.getScientificName().equals(speciesName))
			.forEach( data -> {
				List<String> geneList = data.getEnsembleGeneIDs().stream().distinct().collect(Collectors.toList());
				for(String ensemblGeneID : geneList) {
					// If the gene ID is not already in the set (could happen if you're using a pre-existing
					// gene list).  We'll assume that if it a Gene ID is in the list, it's OK. This *might*
					// not be a very good assumption for Production (unless you know the list is fresh),
					// but for testing purposes, it will probably speed things up greatly.
					if (!genesOKWithENSEMBL.contains(ensemblGeneID)) {
						// Check if the gene is "OK" with ENSEMBL.
						// Here, "OK" means the response matches this regexp:
						// .* seq_region_name=\"(1|2|3|4|5|6|7|8|9|10|11|12|13|14|15|16|17|18|19|20|21|22|X|Y|MT)\" .*
						if (ENSEMBLQueryUtil.checkOKWithENSEMBL(ensemblGeneID)) {
							genesOKWithENSEMBL.add(ensemblGeneID);
							// If the buffer has > 1000 genes, write to file.
							synchronized (geneBuffer) {
								geneBuffer.add(ensemblGeneID);
								if (geneBuffer.size() >= 1000 ) {
									logger.info("Dumping genes to file: {}",ensemblGenesFileName);
									geneBuffer.stream().forEach(gene -> {
										try {
											fileWriter.write(gene + "\n");
										} catch (IOException e) {
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
						if (amt % 1000 == 0) {
							long currentTime = System.currentTimeMillis();
							// unlikely, but it happened at least once during testing.
							if (currentTime == startTimeEnsemblLookup) {
								currentTime += 1;
							}
							logger.info(
								"{} genes were checked with ENSEMBL, " +
								"{} were \"OK\"; " +
								"query rate: {} per second",
								amt, size,(double)size / ((currentTime-startTimeEnsemblLookup)/1000.0)
							);
						}
					}
				}
			});
		}
		long currentTimeEnsembl = System.currentTimeMillis();
		logger.info("{} genes were checked with ENSEMBL, {} were \"OK\". Time spent: {}",
			totalEnsemblGeneCount.get(),
			genesOKWithENSEMBL.size(),
			Duration.ofMillis(currentTimeEnsembl - startTimeEnsemblLookup).toString()
		);
		return genesOKWithENSEMBL;
	}
}
