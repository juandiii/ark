package xyz.juandiii.ark.async.http.decorator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.juandiii.ark.core.exceptions.*;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.http.RetryPolicy;
import xyz.juandiii.ark.core.http.Transport;
import xyz.juandiii.ark.core.http.decorator.Retry;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Coverage + behavior test for Retry&lt;CompletableFuture&lt;RawResponse&gt;&gt;
 * + AsyncRetryOps. Replaces the deleted RetryAsyncTransportTest after Option E
 * Nivel 2 cleanup.
 */
@ExtendWith(MockitoExtension.class)
class RetryAsyncDecoratorTest {

    @Mock
    @SuppressWarnings("unchecked")
    Transport<CompletableFuture<RawResponse>> delegate;

    private static final URI TEST_URI = URI.create("https://api.example.com/test");
    private static final Map<String, String> HEADERS = Map.of();
    private static final RawResponse SUCCESS = new RawResponse(200, Map.of(), "ok");
    private static final RawResponse SERVER_ERROR = new RawResponse(503, Map.of(), "unavailable");
    private static final RawResponse RATE_LIMITED = new RawResponse(429, Map.of(), "rate limited");
    private static final RawResponse BAD_REQUEST = new RawResponse(400, Map.of(), "bad");
    private static final RawResponse INTERNAL_ERROR = new RawResponse(500, Map.of(), "error");

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "test-retry-async");
        t.setDaemon(true);
        return t;
    });

    private Transport<CompletableFuture<RawResponse>> retried(RetryPolicy policy) {
        return new Retry<>(delegate, policy, new AsyncRetryOps(SCHEDULER));
    }

    @Test
    void givenSuccessOnFirstAttempt_thenNoRetry() {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(SUCCESS));

        var r = retried(RetryPolicy.defaults()).send("GET", TEST_URI, HEADERS, null, null).join();

        assertEquals(200, r.statusCode());
        verify(delegate, times(1)).send(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenServerError_thenRetriesAndSucceeds() {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(SERVER_ERROR))
                .thenReturn(CompletableFuture.completedFuture(SUCCESS));

        var policy = RetryPolicy.builder().maxAttempts(3).delay(Duration.ofMillis(10)).build();
        var r = retried(policy).send("GET", TEST_URI, HEADERS, null, null).join();

        assertEquals(200, r.statusCode());
        verify(delegate, times(2)).send(anyString(), any(), any(), any(), any());
    }

    @Test
    void given429_thenRetriesAndSucceeds() {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(RATE_LIMITED))
                .thenReturn(CompletableFuture.completedFuture(SUCCESS));

        var policy = RetryPolicy.builder().maxAttempts(3).delay(Duration.ofMillis(10)).build();
        var r = retried(policy).send("GET", TEST_URI, HEADERS, null, null).join();

        assertEquals(200, r.statusCode());
        verify(delegate, times(2)).send(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenMaxAttemptsExhausted_thenFutureCompletesExceptionally() {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(SERVER_ERROR));

        var policy = RetryPolicy.builder().maxAttempts(2).delay(Duration.ofMillis(10)).build();
        var ex = assertThrows(CompletionException.class,
                () -> retried(policy).send("GET", TEST_URI, HEADERS, null, null).join());

        assertInstanceOf(ServiceUnavailableException.class, ex.getCause());
        verify(delegate, times(2)).send(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenNonRetryableStatus_thenFailsImmediately() {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(BAD_REQUEST));

        var ex = assertThrows(CompletionException.class,
                () -> retried(RetryPolicy.defaults()).send("GET", TEST_URI, HEADERS, null, null).join());

        assertInstanceOf(BadRequestException.class, ex.getCause());
        verify(delegate, times(1)).send(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenPostMethod_thenNoRetryByDefault() {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(SERVER_ERROR));

        var ex = assertThrows(CompletionException.class,
                () -> retried(RetryPolicy.defaults())
                        .send("POST", TEST_URI, HEADERS, "body", null).join());

        assertInstanceOf(ServiceUnavailableException.class, ex.getCause());
        verify(delegate, times(1)).send(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenPostWithRetryPostEnabled_thenRetries() {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(SERVER_ERROR))
                .thenReturn(CompletableFuture.completedFuture(SUCCESS));

        var policy = RetryPolicy.builder().retryPost(true)
                .delay(Duration.ofMillis(10)).maxAttempts(3).build();
        var r = retried(policy).send("POST", TEST_URI, HEADERS, "body", null).join();

        assertEquals(200, r.statusCode());
        verify(delegate, times(2)).send(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenTimeoutException_thenRetries() {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new TimeoutException("timeout", null)))
                .thenReturn(CompletableFuture.completedFuture(SUCCESS));

        var policy = RetryPolicy.builder().maxAttempts(3).delay(Duration.ofMillis(10)).build();
        var r = retried(policy).send("GET", TEST_URI, HEADERS, null, null).join();

        assertEquals(200, r.statusCode());
        verify(delegate, times(2)).send(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenConnectionException_thenRetries() {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new ConnectionException("refused", null)))
                .thenReturn(CompletableFuture.completedFuture(SUCCESS));

        var policy = RetryPolicy.builder().maxAttempts(3).delay(Duration.ofMillis(10)).build();
        var r = retried(policy).send("GET", TEST_URI, HEADERS, null, null).join();

        assertEquals(200, r.statusCode());
        verify(delegate, times(2)).send(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenRetryOnExceptionDisabled_thenNoRetryOnTimeout() {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new TimeoutException("timeout", null)));

        var policy = RetryPolicy.builder().maxAttempts(3).delay(Duration.ofMillis(10))
                .retryOnException(false).build();
        var ex = assertThrows(CompletionException.class,
                () -> retried(policy).send("GET", TEST_URI, HEADERS, null, null).join());

        assertInstanceOf(TimeoutException.class, ex.getCause());
        verify(delegate, times(1)).send(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenCustomRetryOnStatuses_thenOnlyRetriesConfigured() {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(INTERNAL_ERROR));

        var policy = RetryPolicy.builder().maxAttempts(3).delay(Duration.ofMillis(10))
                .retryOn(Set.of(503)).build();
        var ex = assertThrows(CompletionException.class,
                () -> retried(policy).send("GET", TEST_URI, HEADERS, null, null).join());

        assertInstanceOf(InternalServerErrorException.class, ex.getCause());
        verify(delegate, times(1)).send(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenBinaryBody_whenRetrySucceedsOnSecondAttempt_thenBytesArePreservedExactly() {
        byte[] body = new byte[]{(byte) 0xFF, (byte) 0xFE, 0x00, 0x01, (byte) 0xC0};
        when(delegate.sendBinary(anyString(), any(), any(), any(byte[].class), any()))
                .thenReturn(CompletableFuture.completedFuture(SERVER_ERROR))
                .thenReturn(CompletableFuture.completedFuture(SUCCESS));

        var policy = RetryPolicy.builder().maxAttempts(3).delay(Duration.ofMillis(10)).build();
        var r = retried(policy).sendBinary("PUT", TEST_URI, HEADERS, body, null).join();

        assertEquals(200, r.statusCode());
        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(delegate, times(2))
                .sendBinary(anyString(), any(), any(), captor.capture(), any());
        for (byte[] captured : captor.getAllValues()) {
            assertArrayEquals(body, captured);
        }
    }

    @Test
    void givenAsyncRetryOps_whenDefaultConstructor_thenWorks() {
        AsyncRetryOps ops = new AsyncRetryOps();
        assertNotNull(ops);
    }
}
