package xyz.juandiii.ark.core.http.decorator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.juandiii.ark.core.exceptions.*;
import xyz.juandiii.ark.core.http.HttpTransport;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.http.RetryPolicy;
import xyz.juandiii.ark.core.http.Transport;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.function.LongConsumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * H3-sync gate (plan 017): verifies Retry&lt;RawResponse&gt; + SyncRetryOps
 * produces the same outcomes as the legacy RetryTransport. Mirrors key
 * scenarios from RetryTransportTest using the new construction.
 */
@ExtendWith(MockitoExtension.class)
class RetryDecoratorTest {

    @Mock HttpTransport delegate;

    private static final URI TEST_URI = URI.create("https://api.example.com/test");
    private static final Map<String, String> HEADERS = Map.of();
    private static final RawResponse SUCCESS = new RawResponse(200, Map.of(), "ok");
    private static final RawResponse SERVER_ERROR = new RawResponse(503, Map.of(), "unavailable");
    private static final RawResponse BAD_REQUEST = new RawResponse(400, Map.of(), "bad");

    private Transport<RawResponse> retried(RetryPolicy policy, LongConsumer sleeper) {
        return new Retry<>(delegate, policy, new SyncRetryOps(sleeper));
    }

    @Test
    void givenSuccessOnFirstAttempt_thenNoRetry() {
        when(delegate.send(anyString(), any(), any(), any(), any())).thenReturn(SUCCESS);

        RawResponse result = retried(RetryPolicy.defaults(), ms -> {})
                .send("GET", TEST_URI, HEADERS, null, null);

        assertEquals(200, result.statusCode());
        verify(delegate, times(1)).send(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenServerError_thenRetriesAndSucceeds() {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenReturn(SERVER_ERROR)
                .thenReturn(SUCCESS);

        RawResponse result = retried(RetryPolicy.defaults(), ms -> {})
                .send("GET", TEST_URI, HEADERS, null, null);

        assertEquals(200, result.statusCode());
        verify(delegate, times(2)).send(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenMaxAttemptsExhausted_thenThrowsApiException() {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenReturn(SERVER_ERROR);

        var policy = RetryPolicy.builder().maxAttempts(3).build();
        assertThrows(ServiceUnavailableException.class, () ->
                retried(policy, ms -> {}).send("GET", TEST_URI, HEADERS, null, null));

        verify(delegate, times(3)).send(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenNonRetryableStatus_thenThrowsImmediately() {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenReturn(BAD_REQUEST);

        assertThrows(BadRequestException.class, () ->
                retried(RetryPolicy.defaults(), ms -> {}).send("GET", TEST_URI, HEADERS, null, null));

        verify(delegate, times(1)).send(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenPostMethod_thenNoRetryByDefault() {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenReturn(SERVER_ERROR);

        assertThrows(ServiceUnavailableException.class, () ->
                retried(RetryPolicy.defaults(), ms -> {}).send("POST", TEST_URI, HEADERS, "body", null));

        verify(delegate, times(1)).send(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenTimeoutException_thenRetries() {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenThrow(new TimeoutException("timeout", null))
                .thenReturn(SUCCESS);

        RawResponse result = retried(RetryPolicy.defaults(), ms -> {})
                .send("GET", TEST_URI, HEADERS, null, null);

        assertEquals(200, result.statusCode());
        verify(delegate, times(2)).send(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenCustomRetryOnStatuses_thenOnlyRetriesConfigured() {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenReturn(new RawResponse(500, Map.of(), "internal"));

        var policy = RetryPolicy.builder().retryOn(Set.of(503)).build();
        assertThrows(InternalServerErrorException.class, () ->
                retried(policy, ms -> {}).send("GET", TEST_URI, HEADERS, null, null));

        verify(delegate, times(1)).send(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenSleeperInterrupted_thenPropagatesAndStopsRetry() {
        when(delegate.send(anyString(), any(), any(), any(), any())).thenReturn(SERVER_ERROR);

        LongConsumer interruptingSleeper = ms -> {
            Thread.currentThread().interrupt();
            throw new RequestInterruptedException("Retry sleep interrupted",
                    new InterruptedException("test"));
        };

        var policy = RetryPolicy.builder().maxAttempts(3).delay(Duration.ofMillis(10)).build();
        assertThrows(RequestInterruptedException.class, () ->
                retried(policy, interruptingSleeper).send("GET", TEST_URI, HEADERS, null, null));

        verify(delegate, times(1)).send(anyString(), any(), any(), any(), any());
        assertTrue(Thread.interrupted());
    }
}
