package org.reactome.util.ensembl;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
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
		private boolean okToRetry = false;
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
		this.logger =
			logger != null ?
			logger :
			LogManager.getLogger();
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
	 * @deprecated use {@link #processResponse(HttpResponse) instead}
	 */
	@Deprecated
	public EnsemblServiceResult processResponse(HttpResponse response, URI originalURI)
	{
		return processResponse(response);
	}

	/**
	 * Processes the HttpResponse from the queried EnsEMBL service, logs relevant information for the end-user, and
	 * returns the important information of the response as an EnsemblServiceResult object
	 * @param response HttpResponse from the EnsEMBL service
	 * @return EnsemblServiceResult object containing the relevant information from the response
	 */
	public EnsemblServiceResult processResponse(HttpResponse response)
	{
		EnsemblServiceResult result = response.containsHeader("Retry-After") ?
			processResponseWithRetryAfter(response) :
			processResponseWhenNotOverQueryQuota(response);

		processXRateLimitRemaining(response);

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

	// This is most likely to happen if we send SO many requests that we used up our quota with the service, and
	// need to wait for it to reset.
	private EnsemblServiceResult processResponseWithRetryAfter(HttpResponse response)
	{
		logger.debug("Response message: {} ; Reason code: {}; Headers: {}",
			response.getStatusLine().toString(),
			response.getStatusLine().getReasonPhrase(),
			getHeaders(response)
		);

		EnsemblServiceResult result = this.new EnsemblServiceResult();
		result.setStatus(response.getStatusLine().getStatusCode());
		result.setWaitTime(processWaitTime(response));
		result.setOkToRetry(timesWaitedThresholdNotExceeded());

		return result;
	}

	// Called with no "Retry-After" header, so we haven't gone over our quota.
	private EnsemblServiceResult processResponseWhenNotOverQueryQuota(HttpResponse response)
	{
		EnsemblServiceResult result = this.new EnsemblServiceResult();
		result.setStatus(response.getStatusLine().getStatusCode());
		switch (response.getStatusLine().getStatusCode())
		{
			case HttpStatus.SC_GATEWAY_TIMEOUT:
				logger.error("Request timed out! {} retries remaining", timeoutRetriesRemaining);

				timeoutRetriesRemaining--;
				if (timeoutRetriesRemaining > 0)
				{
					result.setOkToRetry(true);
				}
				else
				{
					logger.error("No more retries remaining.");
					timeoutRetriesRemaining = 3;
				}
				break;
			case HttpStatus.SC_OK:
				result.setResult(parseContent(response));
				break;
			case HttpStatus.SC_NOT_FOUND:
				logger.error("Response code 404 ('Not found') received: {}",
					response.getStatusLine().getReasonPhrase()
				);
				// If we got 404, don't retry.
				break;
			case HttpStatus.SC_INTERNAL_SERVER_ERROR:
				logger.error("Error 500 detected! Message: {}",response.getStatusLine().getReasonPhrase());
				// If we get 500 error then we should just get  out of here. Maybe throw an exception?
				break;
			case HttpStatus.SC_BAD_REQUEST:
				logger.trace("Response code was 400 ('Bad request'). Message from server: {}", parseContent(response));
				break;
			default:
				// Log any other kind of response.
				result.setResult(parseContent(response));
				logger.info("Unexpected response {} with message: {}",
					response.getStatusLine().getStatusCode(),
					response.getStatusLine().getReasonPhrase()
				);
				break;
		}

		return result;
	}

	private void processXRateLimitRemaining(HttpResponse response)
	{
		if (response.containsHeader("X-RateLimit-Remaining"))
		{
			numRequestsRemaining.set(parseIntegerHeaderValue(response, "X-RateLimit-Remaining"));
			if (numRequestsRemaining.get() % 1000 == 0)
			{
				logger.debug("{} requests remaining", numRequestsRemaining.get());
			}
		}
		else
		{
			logger.warn(
				"No X-RateLimit-Remaining was returned. This is odd. Response message: {} ; "+
					"Headers returned are: {} " + System.lineSeparator() +
					"Last known value for remaining was {}",

				response.getStatusLine().toString(),
				getHeaders(response),
				numRequestsRemaining
			);
		}
	}

	private Duration processWaitTime(HttpResponse response)
	{
		Duration waitTime = Duration.ofSeconds(parseIntegerHeaderValue(response,"Retry-After"));

		logger.warn("The server told us to wait, so we will wait for {} * {} before trying again.",
			waitTime, this.waitMultiplier
		);

		return waitTime.multipliedBy(this.waitMultiplier);
	}

	private boolean timesWaitedThresholdNotExceeded()
	{
		// If we get told to wait >= 5 times, let's just take the hint and stop trying.
		if (this.waitMultiplier >= 5)
		{
			logger.error(
				"I've already waited {} times and I'm STILL getting told to wait. This will be the LAST attempt.",
				this.waitMultiplier
			);

			return false;
		}

		// It's ok to re-query the sevice, as long as you wait for the time the server wants you to wait.
		this.waitMultiplier++;
		return true;
	}

	private String parseContent(HttpResponse response)
	{
		try
		{
			return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
		}
		catch (ParseException | IOException e)
		{
			e.printStackTrace();
			return "";
		}
	}

	private static int parseIntegerHeaderValue(HttpResponse response, String header)
	{
		return Integer.parseInt(response.getHeaders(header)[0].getValue());
	}

	private List<String> getHeaders(HttpResponse response)
	{
		return Arrays.stream(response.getAllHeaders()).map(Object::toString).collect(Collectors.toList());
	}
}