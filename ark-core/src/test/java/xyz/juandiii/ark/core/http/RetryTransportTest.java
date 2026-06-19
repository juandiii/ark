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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongConsumer;

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

    // ---- Interrupt handling during retry sleep ----

    @Test
    void givenInjectedSleeperThrowsRequestInterrupted_whenRetrying_thenPropagatesAndStopsRetry() {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenThrow(new ServiceUnavailableException("unavailable"));

        LongConsumer interruptingSleeper = millis -> {
            Thread.currentThread().interrupt();
            throw new RequestInterruptedException("Retry sleep interrupted",
                    new InterruptedException("test"));
        };
        var policy = RetryPolicy.builder().maxAttempts(3).delay(Duration.ofMillis(10)).build();
        var transport = new RetryTransport(delegate, policy, interruptingSleeper);

        RequestInterruptedException ex = assertThrows(RequestInterruptedException.class,
                () -> transport.send("GET", TEST_URI, HEADERS, null, null));

        assertInstanceOf(InterruptedException.class, ex.getCause());
        // After 1st attempt fails and sleep throws, no further retry attempts must be made
        verify(delegate, times(1)).send(anyString(), any(), any(), any(), any());
        // Clear the flag set by the test sleeper so the JVM doesn't leak it
        assertTrue(Thread.interrupted());
    }

    @Test
    void givenRealSleepIsInterrupted_whenRetrying_thenThrowsRequestInterruptedException() throws Exception {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenThrow(new ServiceUnavailableException("unavailable"));

        // Use the production 2-arg constructor — real Thread.sleep path via threadSleep().
        var policy = RetryPolicy.builder().maxAttempts(3).delay(Duration.ofSeconds(10)).build();
        var transport = new RetryTransport(delegate, policy);

        AtomicReference<Throwable> caught = new AtomicReference<>();
        CountDownLatch started = new CountDownLatch(1);
        Thread worker = new Thread(() -> {
            started.countDown();
            try {
                transport.send("GET", TEST_URI, HEADERS, null, null);
            } catch (Throwable t) {
                caught.set(t);
            }
        }, "interrupt-test-worker");
        worker.setDaemon(true);
        worker.start();

        assertTrue(started.await(2, TimeUnit.SECONDS), "worker must start");
        // Give the worker a moment to fail once and enter the 10s sleep
        Thread.sleep(150);
        worker.interrupt();
        worker.join(3_000);

        assertFalse(worker.isAlive(), "worker should have completed after interrupt");
        assertNotNull(caught.get(), "worker should have thrown");
        assertInstanceOf(RequestInterruptedException.class, caught.get(),
                "expected RequestInterruptedException, got " + caught.get().getClass().getName());
        assertInstanceOf(InterruptedException.class, caught.get().getCause());
    }
}
