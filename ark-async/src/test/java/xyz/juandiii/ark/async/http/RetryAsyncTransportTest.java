package xyz.juandiii.ark.async.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.juandiii.ark.core.exceptions.*;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.http.RetryPolicy;

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

@ExtendWith(MockitoExtension.class)
class RetryAsyncTransportTest {

    @Mock AsyncHttpTransport delegate;

    private static final URI TEST_URI = URI.create("https://api.example.com/test");
    private static final Map<String, String> HEADERS = Map.of();
    private static final RawResponse SUCCESS = new RawResponse(200, Map.of(), "ok");
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "test-retry");
        t.setDaemon(true);
        return t;
    });

    private RetryAsyncTransport transport(RetryPolicy policy) {
        return new RetryAsyncTransport(delegate, policy, SCHEDULER);
    }

    @Test
    void givenSuccessOnFirstAttempt_thenNoRetry() {
        when(delegate.sendAsync(anyString(), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(SUCCESS));

        var result = transport(RetryPolicy.defaults())
                .sendAsync("GET", TEST_URI, HEADERS, null, null).join();

        assertEquals(200, result.statusCode());
        verify(delegate, times(1)).sendAsync(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenServerError_thenRetriesAndSucceeds() {
        when(delegate.sendAsync(anyString(), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new ServiceUnavailableException("unavailable")))
                .thenReturn(CompletableFuture.completedFuture(SUCCESS));

        var policy = RetryPolicy.builder().maxAttempts(3).delay(Duration.ofMillis(50)).build();
        var result = transport(policy).sendAsync("GET", TEST_URI, HEADERS, null, null).join();

        assertEquals(200, result.statusCode());
        verify(delegate, times(2)).sendAsync(anyString(), any(), any(), any(), any());
    }

    @Test
    void given429_thenRetriesAndSucceeds() {
        when(delegate.sendAsync(anyString(), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new TooManyRequestsException("rate limited")))
                .thenReturn(CompletableFuture.completedFuture(SUCCESS));

        var policy = RetryPolicy.builder().maxAttempts(3).delay(Duration.ofMillis(50)).build();
        var result = transport(policy).sendAsync("GET", TEST_URI, HEADERS, null, null).join();

        assertEquals(200, result.statusCode());
        verify(delegate, times(2)).sendAsync(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenMaxAttemptsExhausted_thenFutureCompletesExceptionally() {
        when(delegate.sendAsync(anyString(), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new ServiceUnavailableException("unavailable")));

        var policy = RetryPolicy.builder().maxAttempts(2).delay(Duration.ofMillis(50)).build();
        var future = transport(policy).sendAsync("GET", TEST_URI, HEADERS, null, null);

        var ex = assertThrows(CompletionException.class, future::join);
        assertInstanceOf(ServiceUnavailableException.class, ex.getCause());
        verify(delegate, times(2)).sendAsync(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenNonRetryableStatus_thenFailsImmediately() {
        when(delegate.sendAsync(anyString(), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new BadRequestException("bad")));

        var policy = RetryPolicy.builder().maxAttempts(3).delay(Duration.ofMillis(50)).build();
        var future = transport(policy).sendAsync("GET", TEST_URI, HEADERS, null, null);

        var ex = assertThrows(CompletionException.class, future::join);
        assertInstanceOf(BadRequestException.class, ex.getCause());
        verify(delegate, times(1)).sendAsync(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenPostMethod_thenNoRetryByDefault() {
        when(delegate.sendAsync(anyString(), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new ServiceUnavailableException("unavailable")));

        var policy = RetryPolicy.builder().maxAttempts(3).delay(Duration.ofMillis(50)).build();
        var future = transport(policy).sendAsync("POST", TEST_URI, HEADERS, "body", null);

        var ex = assertThrows(CompletionException.class, future::join);
        assertInstanceOf(ServiceUnavailableException.class, ex.getCause());
        verify(delegate, times(1)).sendAsync(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenPostMethodWithRetryPostEnabled_thenRetries() {
        when(delegate.sendAsync(anyString(), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new ServiceUnavailableException("unavailable")))
                .thenReturn(CompletableFuture.completedFuture(SUCCESS));

        var policy = RetryPolicy.builder().maxAttempts(3).delay(Duration.ofMillis(50)).retryPost(true).build();
        var result = transport(policy).sendAsync("POST", TEST_URI, HEADERS, "body", null).join();

        assertEquals(200, result.statusCode());
        verify(delegate, times(2)).sendAsync(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenTimeoutException_thenRetries() {
        when(delegate.sendAsync(anyString(), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new TimeoutException("timeout", null)))
                .thenReturn(CompletableFuture.completedFuture(SUCCESS));

        var policy = RetryPolicy.builder().maxAttempts(3).delay(Duration.ofMillis(50)).build();
        var result = transport(policy).sendAsync("GET", TEST_URI, HEADERS, null, null).join();

        assertEquals(200, result.statusCode());
        verify(delegate, times(2)).sendAsync(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenConnectionException_thenRetries() {
        when(delegate.sendAsync(anyString(), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new ConnectionException("refused", null)))
                .thenReturn(CompletableFuture.completedFuture(SUCCESS));

        var policy = RetryPolicy.builder().maxAttempts(3).delay(Duration.ofMillis(50)).build();
        var result = transport(policy).sendAsync("GET", TEST_URI, HEADERS, null, null).join();

        assertEquals(200, result.statusCode());
        verify(delegate, times(2)).sendAsync(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenRetryOnExceptionDisabled_thenNoRetryOnTimeout() {
        when(delegate.sendAsync(anyString(), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new TimeoutException("timeout", null)));

        var policy = RetryPolicy.builder().maxAttempts(3).delay(Duration.ofMillis(50))
                .retryOnException(false).build();
        var future = transport(policy).sendAsync("GET", TEST_URI, HEADERS, null, null);

        var ex = assertThrows(CompletionException.class, future::join);
        assertInstanceOf(TimeoutException.class, ex.getCause());
        verify(delegate, times(1)).sendAsync(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenCustomRetryOnStatuses_thenOnlyRetriesConfigured() {
        when(delegate.sendAsync(anyString(), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new InternalServerErrorException("error")));

        var policy = RetryPolicy.builder().maxAttempts(3).delay(Duration.ofMillis(50))
                .retryOn(Set.of(503)).build();
        var future = transport(policy).sendAsync("GET", TEST_URI, HEADERS, null, null);

        var ex = assertThrows(CompletionException.class, future::join);
        assertInstanceOf(InternalServerErrorException.class, ex.getCause());
        verify(delegate, times(1)).sendAsync(anyString(), any(), any(), any(), any());
    }
}
