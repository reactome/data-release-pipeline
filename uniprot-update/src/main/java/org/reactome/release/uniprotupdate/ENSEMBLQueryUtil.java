package org.reactome.release.uniprotupdate;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reactome.util.ensembl.EnsemblServiceResponseProcessor;
import org.reactome.util.ensembl.EnsemblServiceResponseProcessor.EnsemblServiceResult;

class ENSEMBLQueryUtil
{
	private static final Logger logger = LogManager.getLogger();
	
	public static boolean checkOKWithENSEMBL(String ensemblGeneID) throws URISyntaxException
	{
		boolean isOK = false;
		// need to query a URL of the form:
		// https://rest.ensembl.org/lookup/id/ENSG00000204518?content-type=text/xml
		// See the old Perl version of this: EnsEMBLUtils.pm, sub on_EnsEMBL_primary_assembly
		URI uri = new URI("https://rest.ensembl.org/lookup/id/" + ensemblGeneID);
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
				Pattern validSeqRegionPatter = Pattern.compile(".* seq_region_name=\"(1|2|3|4|5|6|7|8|9|10|11|12|13|14|15|16|17|18|19|20|21|22|X|Y|MT)\" .*", Pattern.MULTILINE);
				Matcher m = validSeqRegionPatter.matcher(queryResponse);
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
}
