package org.reactome.util.ensembl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.reactome.util.ensembl.EnsemblServiceResponseProcessor.MAX_TIMES_TO_WAIT;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.reactome.util.ensembl.EnsemblServiceResponseProcessor.EnsemblServiceResult;

@TestInstance(Lifecycle.PER_CLASS)
public class EnsemblServiceResponseProcessorTest {
	private final int TOO_MANY_REQUESTS_STATUS_CODE = 429;
	private final String TOO_MANY_REQUESTS_REASON_PHRASE = "Too Many Requests";
	private final int RETRY_AFTER_VALUE = 5;
	private final int X_RATE_LIMIT_REMAINING_VALUE = 123;
	private final String DUMMY_RESPONSE_CONTENT = "Dummy Content";

	@Mock
	private HttpResponse response;
	@Mock
	private StatusLine statusLine;
	@Mock
	private Logger logger;

	private EnsemblServiceResponseProcessor ensemblServiceResponseProcessor;

	@BeforeAll
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@BeforeEach
	public void createEnsemblServiceResponseProcessor() {
		ensemblServiceResponseProcessor = new EnsemblServiceResponseProcessor(logger);
	}

	@Test
	public void correctEnsemblServiceResultAfterSingleResponseWithRetryAfter() {
		mockResponseWithRetryHeader(TOO_MANY_REQUESTS_STATUS_CODE, TOO_MANY_REQUESTS_REASON_PHRASE);

		EnsemblServiceResult result = ensemblServiceResponseProcessor.processResponseWithRetryAfter(response);

		assertThat("Incorrect status code", result.getStatus(), is(equalTo(TOO_MANY_REQUESTS_STATUS_CODE)));
		assertThat("isOkayToRetry is false", result.isOkToRetry(), is(true));
		assertThat("Wait time is unexpected",
			result.getWaitTime(), is(equalTo(Duration.ofSeconds(RETRY_AFTER_VALUE)))
		);
	}

	@Test
	public void correctEnsemblServiceResultAfterMaximumNumberOfResponsesWithRetryAfter() {
		mockResponseWithRetryHeader(TOO_MANY_REQUESTS_STATUS_CODE, TOO_MANY_REQUESTS_REASON_PHRASE);

		// First call the method up to the maximum number of times
		for (int i = 1; i < MAX_TIMES_TO_WAIT; i++) {
			ensemblServiceResponseProcessor.processResponseWithRetryAfter(response);
		}

		final int expectedMultiplierAfterMaximumNumberOfResponsesWithRetry = MAX_TIMES_TO_WAIT;
		final int expectedWaitTime = RETRY_AFTER_VALUE * expectedMultiplierAfterMaximumNumberOfResponsesWithRetry;

		EnsemblServiceResult result = ensemblServiceResponseProcessor.processResponseWithRetryAfter(response);
		assertThat("Incorrect status code", result.getStatus(), is(equalTo(TOO_MANY_REQUESTS_STATUS_CODE)));
		assertThat("isOkayToRetry is true", result.isOkToRetry(), is(false));
		assertThat("Wait time is unexpected", result.getWaitTime(),
			is(equalTo(Duration.ofSeconds(expectedWaitTime))));
	}

	@Test
	public void correctEnsemblServiceResultAfterOkayResponse() {
		final int okayStatusCode = HttpStatus.SC_OK;
		final String okayReasonPhrase = "OK";

		mockResponse(okayStatusCode, okayReasonPhrase);
		mockResponseEntityWithContent();

		EnsemblServiceResult result = ensemblServiceResponseProcessor.processResponseWhenNotOverQueryQuota(response);

		assertThat("Incorrect status code", result.getStatus(), is(equalTo(okayStatusCode)));
		assertThat("Result content is unexpected", result.getResult(), is(equalTo(DUMMY_RESPONSE_CONTENT)));
		assertThat("isOkayToRetry is true", result.isOkToRetry(), is(equalTo(false)));
		assertThat("Wait time is not zero", result.getWaitTime(), is(equalTo(Duration.ZERO)));
	}

	@Test
	public void correctEnsemblServiceResultAfterGatewayTimeoutResponse() {
		final int gatewayTimeoutStatusCode = HttpStatus.SC_GATEWAY_TIMEOUT;
		final String gatewayTimeoutReasonPhrase = "Gateway Time-out";

		mockResponse(gatewayTimeoutStatusCode, gatewayTimeoutReasonPhrase);

		EnsemblServiceResult result = ensemblServiceResponseProcessor.processResponseWhenNotOverQueryQuota(response);

		assertThat("Incorrect status code", result.getStatus(), is(equalTo(gatewayTimeoutStatusCode)));
		assertThat("Non-empty result", result.getResult(), is(equalTo(StringUtils.EMPTY)));
		assertThat("isOkayToRetry is false", result.isOkToRetry(), is(equalTo(true)));
		assertThat("Wait time is not zero", result.getWaitTime(), is(equalTo(Duration.ZERO)));
	}

	@Test
	public void correctEnsemblServiceResultAfterGatewayTimeoutResponseAndMaximumTimeoutRetriesAttempted() {
		final int gatewayTimeoutStatusCode = HttpStatus.SC_GATEWAY_TIMEOUT;
		final String gatewayTimeoutReasonPhrase = "Gateway Time-out";
		final int allowedTimeoutRetries = ensemblServiceResponseProcessor.getTimeoutRetriesRemaining();

		mockResponse(gatewayTimeoutStatusCode, gatewayTimeoutReasonPhrase);

		// Use all allowed timeout retries
		for (int i = 1; i < allowedTimeoutRetries; i++) {
			ensemblServiceResponseProcessor.processResponseWhenNotOverQueryQuota(response);
		}

		EnsemblServiceResult result = ensemblServiceResponseProcessor.processResponseWhenNotOverQueryQuota(response);
		assertThat("Incorrect status code", result.getStatus(), is(equalTo(gatewayTimeoutStatusCode)));
		assertThat("Non-empty result", result.getResult(), is(equalTo(StringUtils.EMPTY)));
		assertThat("isOkayToRetry is true", result.isOkToRetry(), is(equalTo(false)));
		assertThat("Wait time is not zero", result.getWaitTime(), is(equalTo(Duration.ZERO)));

		assertThat("Timeout retries not reset",
			ensemblServiceResponseProcessor.getTimeoutRetriesRemaining(), is(equalTo(allowedTimeoutRetries)));
	}

	@Test
	public void correctEnsemblServiceResultAfterNotFoundResponse() {
		final int notFoundStatusCode = HttpStatus.SC_NOT_FOUND;
		final String notFoundReasonPhrase = "Not Found";

		mockResponse(notFoundStatusCode, notFoundReasonPhrase);

		EnsemblServiceResult result = ensemblServiceResponseProcessor.processResponseWhenNotOverQueryQuota(response);

		assertThat("Incorrect status code", result.getStatus(), is(equalTo(notFoundStatusCode)));
		assertThat("Non-empty result", result.getResult(), is(equalTo(StringUtils.EMPTY)));
		assertThat("isOkayToRetry is true", result.isOkToRetry(), is(equalTo(false)));
		assertThat("Wait time is not zero", result.getWaitTime(), is(equalTo(Duration.ZERO)));
	}

	@Test
	public void correctEnsemblServiceResultAfterInternalServerErrorResponse() {
		final int internalServerErrorStatusCode = HttpStatus.SC_INTERNAL_SERVER_ERROR;
		final String internalServerErrorReasonPhrase = "Internal Server Error";

		mockResponse(internalServerErrorStatusCode, internalServerErrorReasonPhrase);

		EnsemblServiceResult result = ensemblServiceResponseProcessor.processResponseWhenNotOverQueryQuota(response);

		assertThat("Incorrect status code", result.getStatus(), is(equalTo(internalServerErrorStatusCode)));
		assertThat("Non-empty result", result.getResult(), is(equalTo(StringUtils.EMPTY)));
		assertThat("isOkayToRetry is true", result.isOkToRetry(), is(equalTo(false)));
		assertThat("Wait time is not zero", result.getWaitTime(), is(equalTo(Duration.ZERO)));
	}

	@Test
	public void correctEnsemblServiceResultAfterBadRequestResponse() {
		final int badRequestStatusCode = HttpStatus.SC_BAD_REQUEST;
		final String badRequestReasonPhrase = "Bad Request";

		mockResponse(badRequestStatusCode, badRequestReasonPhrase);
		mockResponseEntityWithContent();

		EnsemblServiceResult result = ensemblServiceResponseProcessor.processResponseWhenNotOverQueryQuota(response);

		assertThat("Incorrect status code", result.getStatus(), is(equalTo(badRequestStatusCode)));
		assertThat("Non-empty result", result.getResult(), is(equalTo(StringUtils.EMPTY)));
		assertThat("isOkayToRetry is true", result.isOkToRetry(), is(equalTo(false)));
		assertThat("Wait time is not zero", result.getWaitTime(), is(equalTo(Duration.ZERO)));
	}

	@Test
	public void correctEnsemblServiceResultAfterUnexpectedResponse() {
		final int movedPermanentlyStatusCode = HttpStatus.SC_MOVED_PERMANENTLY;
		final String movedPermanentlyReasonPhrase = "Moved Permanently";

		mockResponse(movedPermanentlyStatusCode, movedPermanentlyReasonPhrase);
		mockResponseEntityWithContent();

		EnsemblServiceResult result = ensemblServiceResponseProcessor.processResponseWhenNotOverQueryQuota(response);

		assertThat("Incorrect status code", result.getStatus(), is(equalTo(movedPermanentlyStatusCode)));
		assertThat("Result content is unexpected", result.getResult(), is(equalTo(DUMMY_RESPONSE_CONTENT)));
		assertThat("isOkayToRetry is true", result.isOkToRetry(), is(equalTo(false)));
		assertThat("Wait time is not zero", result.getWaitTime(), is(equalTo(Duration.ZERO)));
	}

	@Test
	public void correctNumberOfRequestsRemainingSetAfterResponseWithXRateLimit() {
		final int okayStatusCode = HttpStatus.SC_OK;
		final String okayReasonPhrase = "OK";

		mockResponse(okayStatusCode, okayReasonPhrase);
		mockXRateLimitHeader();

		ensemblServiceResponseProcessor.processXRateLimitRemaining(response);

		assertThat(
			EnsemblServiceResponseProcessor.getNumRequestsRemaining(), is(equalTo(X_RATE_LIMIT_REMAINING_VALUE))
		);
	}

	private void mockResponseWithRetryHeader(int statusCode, String reasonPhrase) {
		mockResponse(statusCode, reasonPhrase);
		mockRetryHeader();
	}

	private void mockResponse(int statusCode, String reasonPhrase) {
		Mockito.when(response.getStatusLine()).thenReturn(statusLine);
		Mockito.when(statusLine.getStatusCode()).thenReturn(statusCode);
		Mockito.when(statusLine.getReasonPhrase()).thenReturn(reasonPhrase);
	}

	private void mockRetryHeader() {
		final String retryAfterHeaderName = "Retry-After";
		final String retryAfterHeaderValue = Integer.toString(RETRY_AFTER_VALUE);
		final Header[] headers = new Header[] {new BasicHeader(retryAfterHeaderName, retryAfterHeaderValue)};
		Mockito.when(response.getAllHeaders()).thenReturn(headers);
		Mockito.when(response.getHeaders(retryAfterHeaderName)).thenReturn(headers);
	}

	private void mockXRateLimitHeader() {
		final String xRateLimitHeaderName = "X-RateLimit-Remaining";
		final String xRateLimitHeaderValue = Integer.toString(X_RATE_LIMIT_REMAINING_VALUE);
		final Header[] headers = new Header[] {new BasicHeader(xRateLimitHeaderName, xRateLimitHeaderValue)};
		Mockito.when(response.containsHeader(xRateLimitHeaderName)).thenReturn(true);
		Mockito.when(response.getAllHeaders()).thenReturn(headers);
		Mockito.when(response.getHeaders(xRateLimitHeaderName)).thenReturn(headers);
	}

	private void mockResponseEntityWithContent() {
		final BasicHttpEntity entity = new BasicHttpEntity();
		final InputStream content = new ByteArrayInputStream(DUMMY_RESPONSE_CONTENT.getBytes());

		entity.setContent(content);
		entity.setContentLength(DUMMY_RESPONSE_CONTENT.getBytes().length);
		Mockito.when(response.getEntity()).thenReturn(entity);
	}
}