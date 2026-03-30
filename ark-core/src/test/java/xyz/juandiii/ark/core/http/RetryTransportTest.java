package xyz.juandiii.ark.core.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.juandiii.ark.core.exceptions.*;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetryTransportTest {

    @Mock HttpTransport delegate;

    private static final URI TEST_URI = URI.create("https://api.example.com/test");
    private static final Map<String, String> HEADERS = Map.of();
    private static final RawResponse SUCCESS = new RawResponse(200, Map.of(), "ok");

    private RetryTransport transport(RetryPolicy policy) {
        List<Long> delays = new ArrayList<>();
        return new RetryTransport(delegate, policy, delays::add);
    }

    private RetryTransport transportWithDelays(RetryPolicy policy, List<Long> delays) {
        return new RetryTransport(delegate, policy, delays::add);
    }

    @Test
    void givenSuccessOnFirstAttempt_thenNoRetry() {
        when(delegate.send(anyString(), any(), any(), any(), any())).thenReturn(SUCCESS);

        var result = transport(RetryPolicy.defaults()).send("GET", TEST_URI, HEADERS, null, null);

        assertEquals(200, result.statusCode());
        verify(delegate, times(1)).send(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenServerError_thenRetriesAndSucceeds() {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenThrow(new ServiceUnavailableException("unavailable"))
                .thenReturn(SUCCESS);

        var result = transport(RetryPolicy.defaults()).send("GET", TEST_URI, HEADERS, null, null);

        assertEquals(200, result.statusCode());
        verify(delegate, times(2)).send(anyString(), any(), any(), any(), any());
    }

    @Test
    void given429_thenRetriesAndSucceeds() {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenThrow(new TooManyRequestsException("rate limited"))
                .thenReturn(SUCCESS);

        var result = transport(RetryPolicy.defaults()).send("GET", TEST_URI, HEADERS, null, null);

        assertEquals(200, result.statusCode());
        verify(delegate, times(2)).send(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenMaxAttemptsExhausted_thenThrowsOriginalException() {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenThrow(new ServiceUnavailableException("unavailable"));

        var policy = RetryPolicy.builder().maxAttempts(3).build();
        assertThrows(ServiceUnavailableException.class, () ->
                transport(policy).send("GET", TEST_URI, HEADERS, null, null));

        verify(delegate, times(3)).send(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenNonRetryableStatus_thenThrowsImmediately() {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenThrow(new BadRequestException("bad request"));

        assertThrows(BadRequestException.class, () ->
                transport(RetryPolicy.defaults()).send("GET", TEST_URI, HEADERS, null, null));

        verify(delegate, times(1)).send(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenTimeoutException_thenRetries() {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenThrow(new TimeoutException("timeout", null))
                .thenReturn(SUCCESS);

        var result = transport(RetryPolicy.defaults()).send("GET", TEST_URI, HEADERS, null, null);

        assertEquals(200, result.statusCode());
        verify(delegate, times(2)).send(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenConnectionException_thenRetries() {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenThrow(new ConnectionException("refused", null))
                .thenReturn(SUCCESS);

        var result = transport(RetryPolicy.defaults()).send("GET", TEST_URI, HEADERS, null, null);

        assertEquals(200, result.statusCode());
        verify(delegate, times(2)).send(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenRetryOnExceptionDisabled_thenNoRetryOnTimeout() {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenThrow(new TimeoutException("timeout", null));

        var policy = RetryPolicy.builder().retryOnException(false).build();
        assertThrows(TimeoutException.class, () ->
                transport(policy).send("GET", TEST_URI, HEADERS, null, null));

        verify(delegate, times(1)).send(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenPostMethod_thenNoRetryByDefault() {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenThrow(new ServiceUnavailableException("unavailable"));

        assertThrows(ServiceUnavailableException.class, () ->
                transport(RetryPolicy.defaults()).send("POST", TEST_URI, HEADERS, "body", null));

        verify(delegate, times(1)).send(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenPostMethodWithRetryPostEnabled_thenRetries() {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenThrow(new ServiceUnavailableException("unavailable"))
                .thenReturn(SUCCESS);

        var policy = RetryPolicy.builder().retryPost(true).build();
        var result = transport(policy).send("POST", TEST_URI, HEADERS, "body", null);

        assertEquals(200, result.statusCode());
        verify(delegate, times(2)).send(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenPatchMethod_thenNoRetryByDefault() {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenThrow(new ServiceUnavailableException("unavailable"));

        assertThrows(ServiceUnavailableException.class, () ->
                transport(RetryPolicy.defaults()).send("PATCH", TEST_URI, HEADERS, "body", null));

        verify(delegate, times(1)).send(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenCustomRetryOnStatuses_thenOnlyRetriesConfigured() {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenThrow(new InternalServerErrorException("error"));

        var policy = RetryPolicy.builder().retryOn(Set.of(503)).build();
        assertThrows(InternalServerErrorException.class, () ->
                transport(policy).send("GET", TEST_URI, HEADERS, null, null));

        verify(delegate, times(1)).send(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenRetries_thenDelaysAreInExpectedRange() {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenThrow(new ServiceUnavailableException("unavailable"))
                .thenThrow(new ServiceUnavailableException("unavailable"))
                .thenReturn(SUCCESS);

        List<Long> delays = new ArrayList<>();
        var policy = RetryPolicy.builder()
                .maxAttempts(3)
                .delay(Duration.ofMillis(1000))
                .multiplier(2.0)
                .build();

        transportWithDelays(policy, delays).send("GET", TEST_URI, HEADERS, null, null);

        assertEquals(2, delays.size());
        // Attempt 1: base=1000, range=[500, 1000]
        assertTrue(delays.get(0) >= 500 && delays.get(0) <= 1000,
                "First delay should be in [500, 1000] but was " + delays.get(0));
        // Attempt 2: base=2000, range=[1000, 2000]
        assertTrue(delays.get(1) >= 1000 && delays.get(1) <= 2000,
                "Second delay should be in [1000, 2000] but was " + delays.get(1));
    }

    @Test
    void givenSendBinary_thenRetriesOnServerError() {
        when(delegate.sendBinary(anyString(), any(), any(), any(), any()))
                .thenThrow(new ServiceUnavailableException("unavailable"))
                .thenReturn(SUCCESS);

        var result = transport(RetryPolicy.defaults()).sendBinary("GET", TEST_URI, HEADERS, new byte[]{1, 2, 3}, null);

        assertEquals(200, result.statusCode());
        verify(delegate, times(2)).sendBinary(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenSendBinary_thenExhaustsRetries() {
        when(delegate.sendBinary(anyString(), any(), any(), any(), any()))
                .thenThrow(new ServiceUnavailableException("unavailable"));

        var policy = RetryPolicy.builder().maxAttempts(2).build();
        assertThrows(ServiceUnavailableException.class, () ->
                transport(policy).sendBinary("GET", TEST_URI, HEADERS, new byte[]{1}, null));

        verify(delegate, times(2)).sendBinary(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenSendBinaryPost_thenNoRetryByDefault() {
        when(delegate.sendBinary(anyString(), any(), any(), any(), any()))
                .thenThrow(new ServiceUnavailableException("unavailable"));

        assertThrows(ServiceUnavailableException.class, () ->
                transport(RetryPolicy.defaults()).sendBinary("POST", TEST_URI, HEADERS, new byte[]{1}, null));

        verify(delegate, times(1)).sendBinary(anyString(), any(), any(), any(), any());
    }
}
