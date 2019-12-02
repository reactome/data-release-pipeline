package org.reactome.util.ensembl;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This is used to process web service responses from EnsEMBL. This code was copied from AddLinks,
 * org.reactome.addlinks.dataretrieval.ensembl.EnsemblServiceResponseProcessor
 * @author sshorser
 */
public final class EnsemblServiceResponseProcessor
{

	/**
	 * Contains the relevant information of the HTTP response from a query to EnsEMBL's service (e.g. status of the
	 * response, if the request should be retried, time to wait before retrying, content of the response if successful)
	 */
	public class EnsemblServiceResult
	{
		private Duration waitTime = Duration.ZERO;
		private String result;
		private boolean okToRetry;
		private int status;

		/**
		 * Retrieves the Duration object describing the amount of time to wait before retrying the request to the
		 * EnsEMBL service
		 * @return Time to wait before retrying request (as a Duration object)
		 */
		public Duration getWaitTime()
		{
			return this.waitTime;
		}

		/**
		 * Sets the duration object describing the amount of time to wait before retrying the request to the EnsEMBL
		 * service
		 * @param waitTime Wait time as Duration object
		 */
		public void setWaitTime(Duration waitTime)
		{
			this.waitTime = waitTime;
		}

		/**
		 * Retrieves the content of the response from the EnsEMBL service
		 * @return Content of the response as a String (empty String if no content)
		 */
		public String getResult()
		{
			return this.result;
		}

		/**
		 * Sets the content of the response from the EnsEMBL service
		 * @param result Content of the response as a String (empty String if no content)
		 */
		public void setResult(String result)
		{
			this.result = result;
		}

		/**
		 * Retrieves if it is permitted to retry the request to the EnsEMBL service
		 * @return true if okay to retry the request; false otherwise
		 */
		public boolean isOkToRetry()
		{
			return this.okToRetry;
		}

		/**
		 * Sets if it is permitted to retry to the request to the EnsEMBL service
		 * @param okToRetry true if okay to retry the request; false otherwise
		 */
		public void setOkToRetry(boolean okToRetry)
		{
			this.okToRetry = okToRetry;
		}

		/**
		 * Retrieves the <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status">HTTP status code</a> of
		 * the response from the EnsEMBL service
		 * @return Response status code from the EnsEMBL service
		 */
		public int getStatus()
		{
			return this.status;
		}

		/**
		 * Sets the <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status">HTTP status code</a> of
		 * the response from the EnsEMBL service
		 * @param status Response status code from the EnsEMBL service
		 */
		public void setStatus(int status)
		{
			this.status = status;
		}
	}

	private int waitMultiplier = 1;

	// Assume a quota of 10 to start. This will get set properly with every response from the service.
	private static final AtomicInteger numRequestsRemaining = new AtomicInteger(10);

	private Logger logger ;

	// This can't be static because each request could have a different timeoutRetries counter.
	private int timeoutRetriesRemaining = 3;

	/**
	 * Constructs a new EnsemblServiceResponseProcessor object with the specified logger
	 * to record information about HttpResponse objects from the EnsEMBL service
	 * @param logger Logger object to record information about processed responses
	 */
	public EnsemblServiceResponseProcessor(Logger logger)
	{
		if (logger!=null)
		{
			this.logger = logger;
		}
		else
		{
			this.logger = LogManager.getLogger();
		}
	}

	/**
	 * Constructs a new EnsemblServiceResponseProcessor object with a default logger
	 * to record information about HttpResponse objects from the EnsEMBL service
	 */
	public EnsemblServiceResponseProcessor()
	{
		this(null);
	}

	/**
	 * Processes the HttpResponse from the queried EnsEMBL service, logs relevant information for the end-user, and
	 * returns the important information of the response as an EnsemblServiceResult object
	 * @param response HttpResponse from the EnsEMBL service
	 * @param originalURI The URI of the EnsEMBL service queried (e.g rest.ensembl.org or rest.ensemblgenomes.org)
	 * @return EnsemblServiceResult object containing the relevant information from the response
	 */
	public EnsemblServiceResult processResponse(HttpResponse response, URI originalURI)
	{
		EnsemblServiceResult result = this.new EnsemblServiceResult();
		result.setStatus(response.getStatusLine().getStatusCode());
		boolean okToQuery = false;
		// First check to see if we got a "Retry-After" header. This is most likely to happen if we send SO many
		// requests that we used up our quota with the service, and need to wait for it to reset.
		if ( response.containsHeader("Retry-After") )
		{
			logger.debug("Response message: {} ; Reason code: {}; Headers: {}",
				response.getStatusLine().toString(),
				response.getStatusLine().getReasonPhrase(),
				Arrays.stream(response.getAllHeaders()).map(Object::toString).collect(Collectors.toList())
			);


			Duration waitTime = Duration.ofSeconds(Integer.parseInt(response.getHeaders("Retry-After")[0].getValue()));

			logger.warn("The server told us to wait, so we will wait for {} * {} before trying again.",
				waitTime, this.waitMultiplier
			);

			result.setWaitTime(waitTime.multipliedBy(this.waitMultiplier));
			this.waitMultiplier ++;
			// If we get told to wait > 5 times, let's just take the hint and stop trying.
			if (this.waitMultiplier >= 5)
			{
				logger.error(
					"I've already waited {} times and I'm STILL getting told to wait. This will be the LAST attempt.",
					this.waitMultiplier
				);
				okToQuery = false;
			}
			else
			{
				// It's ok to re-query the sevice, as long as you wait for the time the server wants you to wait.
				okToQuery = true;
			}
		}
		// Else... no "Retry-After" so we haven't gone over our quota.
		else
		{
			String content = "";
			switch (response.getStatusLine().getStatusCode())
			{
				case HttpStatus.SC_OK:
					try
					{
						content = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
					}
					catch (ParseException | IOException e)
					{
						e.printStackTrace();
					}
					result.setResult(content);
					okToQuery = false;
					break;
				case HttpStatus.SC_NOT_FOUND:
					logger.error("Response code 404 (\"Not found\") received: {}",
						response.getStatusLine().getReasonPhrase()
					);
					// If we got 404, don't retry.
					okToQuery = false;
					break;
				case HttpStatus.SC_INTERNAL_SERVER_ERROR:
					logger.error("Error 500 detected! Message: {}",response.getStatusLine().getReasonPhrase());
					// If we get 500 error then we should just get  out of here. Maybe throw an exception?
					okToQuery = false;
					break;
				case HttpStatus.SC_BAD_REQUEST:
					String s = "";
					try
					{
						s = EntityUtils.toString(response.getEntity());
					}
					catch (ParseException | IOException e)
					{
						e.printStackTrace();
					}
					logger.trace("Response code was 400 (\"Bad request\"). Message from server: {}", s);
					okToQuery = false;
					break;
				case HttpStatus.SC_GATEWAY_TIMEOUT:
					timeoutRetriesRemaining--;
					logger.error("Request timed out! {} retries remaining", timeoutRetriesRemaining);
					if (timeoutRetriesRemaining > 0)
					{
						okToQuery = true;
					}
					else
					{
						logger.error("No more retries remaining.");
						timeoutRetriesRemaining = 3;
						okToQuery = false;
					}
					break;
				default:
					// Log any other kind of response.
					okToQuery = false;
					try
					{
						content = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
					}
					catch (ParseException | IOException e)
					{
						e.printStackTrace();
					}
					result.setResult(content);
					logger.info("Unexpected response {} with message: {}",response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
					break;
			}
		}
		result.setOkToRetry(okToQuery);
		if (response.containsHeader("X-RateLimit-Remaining"))
		{
			int numRequestsRemaining = Integer.parseInt(response.getHeaders("X-RateLimit-Remaining")[0].getValue());
			EnsemblServiceResponseProcessor.numRequestsRemaining.set(numRequestsRemaining);
			numRequestsRemaining = EnsemblServiceResponseProcessor.numRequestsRemaining.get();
			if (numRequestsRemaining % 1000 == 0)
			{
				logger.debug("{} requests remaining", numRequestsRemaining);
			}
		}
		else
		{
			logger.warn(
				"No X-RateLimit-Remaining was returned. This is odd. Response message: {} ; "
					+ "Headers returned are: {}\nLast known value for remaining was {}",
				response.getStatusLine().toString(),
				Arrays.stream(response.getAllHeaders()).map(Object::toString).collect(Collectors.toList()),
				EnsemblServiceResponseProcessor.numRequestsRemaining
			);
		}
		return result;
	}

	/**
	 * Returns the number of requests that may still be made to the EnsEMBL service in the current time window
	 * @return Number of requests that can still be made until the the time window is refreshed
	 */
	public static int getNumRequestsRemaining()
	{
		return EnsemblServiceResponseProcessor.numRequestsRemaining.get();
	}
}