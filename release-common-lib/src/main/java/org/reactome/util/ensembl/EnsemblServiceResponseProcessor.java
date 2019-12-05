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
	public static final int MAX_TIMES_TO_WAIT = 5;
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

	private Logger logger;
	private int waitMultiplier = 1;
	// This can't be static because each request could have a different timeoutRetries counter.
	private int timeoutRetriesRemaining;

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

		initializeTimeoutRetriesRemaining();
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

	/**
	 * Returns the factor by which the response's recommended wait time is multiplied.  The value starts at 1 and
	 * is incremented each time the response to the request is to wait.  This gives the server some buffer time before
	 * the request is retried.
	 * @return Factor by which to multiply the server's response recommended wait time
	 */
	public int getWaitMultiplier()
	{
		return this.waitMultiplier;
	}

	/**
	 * Increase the factor by which the response's recommended wait time it multiplied by one.
	 */
	private void incrementWaitMultiplier()
	{
		this.waitMultiplier++;
	}

	/**
	 * Returns the number of request retries remaining when the server's response is a gateway timeout (status code
	 * 504)
	 * @return Number of retries remaining
	 */
	public int getTimeoutRetriesRemaining()
	{
		return this.timeoutRetriesRemaining;
	}

	/**
	 * Sets/resets the starting number of request retries permitted when the server's response is a gateway timeout
	 * (status code 504)
	 */
	private void initializeTimeoutRetriesRemaining()
	{
		this.timeoutRetriesRemaining = 3;
	}

	/**
	 * Process the query's response object when the response contains the header "Retry-After" which is most likely
	 * to happen if too many requests are sent in a fixed time, using up our quota with the service resulting in a
	 * need to wait before more requests can be sent.
	 *
	 * This method will create and return an EnsemblServiceResult object containing the status code of the response,
	 * if it is okay to retry the request {@link #timesWaitedThresholdNotMet()}, and the duration of time to wait
	 * before retrying.
	 *
	 * The response's status message, reason phrase, and headers will also be logged for debugging.
	 * @param response Response object to process
	 * @return EnsemblServiceResult object with the response status, isOkToRetry, and wait time to retry set
	 */
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
		result.setOkToRetry(timesWaitedThresholdNotMet());

		if (result.isOkToRetry())
		{
			incrementWaitMultiplier();
		}

		return result;
	}

	/**
	 * Processes the query's response object when the response does not contain the header "Retry-After".  This method
	 * will attempt to retrieve the content, and return it as the result value in an EnsemblServiceResult object.
	 *
	 * Certain HTTP response error codes (e.g. 400, 404, 500, 504) will cause the error to be logged and a
	 * EnsemblServiceResult object without content set in the result value to be returned.
	 * @param response Response object to process
	 * @return EnsemblServiceResult object with the content set as its result value if the content was obtained
	 */
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
					initializeTimeoutRetriesRemaining();
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

	/**
	 * Sets the number of requests remaining the server will permit based on the response header
	 * "X-RateLimit-Remaining".
	 *
	 * The number of requests remaining will be logged for debugging if it is a multiple of 1000.  If no
	 * "X-RateLimit-Remaining" header is found in the response object, its absence will be logged along with
	 * the HTTP response code received, the response headers received, and the last known number of requests
	 * remaining.
	 * @param response Response object from which to obtain the number of requests remaining
	 */
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

	/**
	 * Returns the duration to wait before retrying the query.  The wait time is determined from the "Retry-After"
	 * header in the query response and is multiplied by the number of times the query has been requested to wait.
	 * This is to give the increasing server buffer time on each failure requesting the query waits.
	 * @param response Response object from which to obtain the base wait time
	 * @return Wait time as Duration object ("Retry-After" value multiplied by the number of times the query has
	 * requested to wait).
	 */
	private Duration processWaitTime(HttpResponse response)
	{
		Duration waitTime = Duration.ofSeconds(parseIntegerHeaderValue(response,"Retry-After"));

		logger.warn("The server told us to wait, so we will wait for {} * {} before trying again.",
			waitTime, getWaitMultiplier()
		);

		return waitTime.multipliedBy(getWaitMultiplier());
	}

	/**
	 * Returns <code>true</code> if the number of times the query the object for this class has been told to wait is
	 * less than the threshold (set to a maximum of 5); <code>false</code> is returned otherwise and the maximum
	 * number of times waiting being reached is logged.
	 *
	 * @return <code>true</code> if the number of times query has been told to wait is less than the threshold;
	 * <code>false</code> otherwise.
	 */
	private boolean timesWaitedThresholdNotMet()
	{
		// If we get told to wait >= 5 times, let's just take the hint and stop trying.
		if (getWaitMultiplier() >= MAX_TIMES_TO_WAIT)
		{
			logger.error(
				"I've already waited {} times and I'm STILL getting told to wait. This will be the LAST attempt.",
				this.waitMultiplier
			);

			return false;
		}

		return true;
	}

	/**
	 * Parses and returns the content from the response object passed.  Content is parsed as UTF-8.  A stacktrace is
	 * printed to STDERR and an empty String returned if an exception occurs during parsing of the response content.
	 * @param response Response object to query for content
	 * @return Content as String from the response object passed.  An empty String if an exception occurs during
	 * parsing of the content.
	 */
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

	/**
	 * Returns the value of a header, which is an integer, as an integer.
	 * @param response Response object to query for headers
	 * @param header Specific header for which to get the value (which should be an integer)
	 * @return Integer value of the header passed for the response object passed
	 */
	private static int parseIntegerHeaderValue(HttpResponse response, String header) throws NumberFormatException
	{
		return Integer.parseInt(response.getHeaders(header)[0].getValue());
	}

	/**
	 * Returns the headers of the response object as a list of Strings
	 * @param response Response object from which to get the headers
	 * @return List of Strings representing the headers from the passed response object
	 */
	private List<String> getHeaders(HttpResponse response)
	{
		return Arrays.stream(response.getAllHeaders()).map(Object::toString).collect(Collectors.toList());
	}
}