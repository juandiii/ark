package xyz.juandiii.ark.vertx.http.decorator;

import io.vertx.core.Future;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.juandiii.ark.core.exceptions.BadRequestException;
import xyz.juandiii.ark.core.exceptions.ServiceUnavailableException;
import xyz.juandiii.ark.core.exceptions.TimeoutException;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.http.RetryPolicy;
import xyz.juandiii.ark.core.http.Transport;
import xyz.juandiii.ark.core.http.decorator.Retry;
import xyz.juandiii.ark.vertx.http.VertxHttpTransport;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VertxRetryOpsTest {

    @Mock VertxHttpTransport delegate;

    private static final URI TEST_URI = URI.create("https://api.example.com/test");
    private static final Map<String, String> HEADERS = Map.of();
    private static final RawResponse SUCCESS = new RawResponse(200, Map.of(), "ok");
    private static final RawResponse SERVER_ERROR = new RawResponse(503, Map.of(), "unavailable");
    private static final RawResponse BAD_REQUEST = new RawResponse(400, Map.of(), "bad");

    private Transport<Future<RawResponse>> retried(RetryPolicy policy) {
        return new Retry<>(delegate, policy, new VertxRetryOps());
    }

    private RawResponse await(Future<RawResponse> f) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<RawResponse> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        f.onSuccess(r -> { result.set(r); latch.countDown(); })
         .onFailure(t -> { error.set(t); latch.countDown(); });
        assertTrue(latch.await(5, TimeUnit.SECONDS), "future did not complete in time");
        if (error.get() != null) {
            if (error.get() instanceof RuntimeException re) throw re;
            if (error.get() instanceof Exception e) throw e;
            throw new RuntimeException(error.get());
        }
        return result.get();
    }

    private Throwable awaitFailure(Future<RawResponse> f) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        f.onSuccess(r -> latch.countDown())
         .onFailure(t -> { error.set(t); latch.countDown(); });
        assertTrue(latch.await(5, TimeUnit.SECONDS), "future did not complete in time");
        return error.get();
    }

    @Test
    void givenServerError_thenRetriesAndSucceeds() throws Exception {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenReturn(Future.succeededFuture(SERVER_ERROR))
                .thenReturn(Future.succeededFuture(SUCCESS));

        var policy = RetryPolicy.builder().maxAttempts(3).delay(Duration.ofMillis(10)).build();
        RawResponse r = await(retried(policy).send("GET", TEST_URI, HEADERS, null, null));

        assertEquals(200, r.statusCode());
        verify(delegate, times(2)).send(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenMaxAttemptsExhausted_thenFutureFailsWithApiException() throws Exception {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenReturn(Future.succeededFuture(SERVER_ERROR));

        var policy = RetryPolicy.builder().maxAttempts(2).delay(Duration.ofMillis(10)).build();
        Throwable t = awaitFailure(retried(policy).send("GET", TEST_URI, HEADERS, null, null));

        assertInstanceOf(ServiceUnavailableException.class, t);
        verify(delegate, times(2)).send(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenNonRetryableStatus_thenFutureFailsImmediately() throws Exception {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenReturn(Future.succeededFuture(BAD_REQUEST));

        Throwable t = awaitFailure(retried(RetryPolicy.defaults())
                .send("GET", TEST_URI, HEADERS, null, null));

        assertInstanceOf(BadRequestException.class, t);
        verify(delegate, times(1)).send(anyString(), any(), any(), any(), any());
    }

    @Test
    void givenTransportException_thenRetries() throws Exception {
        when(delegate.send(anyString(), any(), any(), any(), any()))
                .thenReturn(Future.failedFuture(new TimeoutException("timeout", null)))
                .thenReturn(Future.succeededFuture(SUCCESS));

        var policy = RetryPolicy.builder().maxAttempts(3).delay(Duration.ofMillis(10)).build();
        RawResponse r = await(retried(policy).send("GET", TEST_URI, HEADERS, null, null));

        assertEquals(200, r.statusCode());
        verify(delegate, times(2)).send(anyString(), any(), any(), any(), any());
    }
}
