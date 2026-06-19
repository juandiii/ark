package xyz.juandiii.ark.core.http;

import xyz.juandiii.ark.core.http.decorator.Retry;
import xyz.juandiii.ark.core.http.decorator.SyncRetryOps;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.function.LongConsumer;

/**
 * Sync transport decorator that retries failed requests with exponential backoff and jitter.
 * Only retries idempotent methods (GET, HEAD, PUT, DELETE, OPTIONS) unless {@code retryPost} is enabled.
 *
 * @deprecated since plan 017. Use {@code transport.with(Retry.of(policy, new SyncRetryOps()))} or
 * construct {@code new Retry<>(delegate, policy, new SyncRetryOps())} directly. This wrapper
 * remains for backward compatibility with existing callers and will be removed in a future plan.
 *
 * @author Juan Diego Lopez V.
 */
@Deprecated(since = "1.0.x", forRemoval = false)
public final class RetryTransport implements HttpTransport {

    private final Retry<RawResponse> inner;

    public RetryTransport(HttpTransport delegate, RetryPolicy policy) {
        this.inner = new Retry<>(delegate, policy, new SyncRetryOps());
    }

    RetryTransport(HttpTransport delegate, RetryPolicy policy, LongConsumer sleeper) {
        this.inner = new Retry<>(delegate, policy, new SyncRetryOps(sleeper));
    }

    @Override
    public RawResponse send(String method, URI uri, Map<String, String> headers,
                            String body, Duration timeout) {
        return inner.send(method, uri, headers, body, timeout);
    }

    @Override
    public RawResponse sendBinary(String method, URI uri, Map<String, String> headers,
                                   byte[] body, Duration timeout) {
        return inner.sendBinary(method, uri, headers, body, timeout);
    }

    /**
     * Package-private static helper preserved for {@code RetryTransportTest.precomputeDelays}
     * (added in plan 010). Delegates to {@link Retry#precomputeDelays}.
     */
    static long[] precomputeDelays(RetryPolicy policy) {
        return Retry.precomputeDelays(policy);
    }
}
