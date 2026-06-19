package xyz.juandiii.ark.async.http;

import xyz.juandiii.ark.async.http.decorator.Adapters;
import xyz.juandiii.ark.async.http.decorator.AsyncRetryOps;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.http.RetryPolicy;
import xyz.juandiii.ark.core.http.decorator.Retry;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Async transport decorator that retries failed requests with exponential backoff and jitter.
 *
 * @deprecated since plan 017. Use the decorator chain directly:
 * {@code Adapters.toAsync(Adapters.fromAsync(transport).with(Retry.of(policy, new AsyncRetryOps())))}.
 * This wrapper remains for backward compatibility with existing callers and will be removed
 * in a future plan.
 *
 * @author Juan Diego Lopez V.
 */
@Deprecated(since = "1.0.x", forRemoval = false)
public final class RetryAsyncTransport implements AsyncHttpTransport {

    private final AsyncHttpTransport inner;

    public RetryAsyncTransport(AsyncHttpTransport delegate, RetryPolicy policy) {
        this.inner = Adapters.toAsync(Adapters.fromAsync(delegate)
                .with(Retry.of(policy, new AsyncRetryOps())));
    }

    public RetryAsyncTransport(AsyncHttpTransport delegate, RetryPolicy policy,
                               ScheduledExecutorService scheduler) {
        this.inner = Adapters.toAsync(Adapters.fromAsync(delegate)
                .with(Retry.of(policy, new AsyncRetryOps(scheduler))));
    }

    @Override
    public CompletableFuture<RawResponse> sendAsync(String method, URI uri,
                                                     Map<String, String> headers,
                                                     String body, Duration timeout) {
        return inner.sendAsync(method, uri, headers, body, timeout);
    }

    @Override
    public CompletableFuture<RawResponse> sendBinaryAsync(String method, URI uri,
                                                           Map<String, String> headers,
                                                           byte[] body, Duration timeout) {
        return inner.sendBinaryAsync(method, uri, headers, body, timeout);
    }

    /**
     * Package-private static helper preserved for {@code RetryAsyncTransportTest.precomputeDelays}
     * (added in plan 010). Delegates to {@link Retry#precomputeDelays}.
     */
    static long[] precomputeDelays(RetryPolicy policy) {
        return Retry.precomputeDelays(policy);
    }
}
