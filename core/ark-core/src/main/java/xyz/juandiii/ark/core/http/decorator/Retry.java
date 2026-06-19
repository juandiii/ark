package xyz.juandiii.ark.core.http.decorator;

import xyz.juandiii.ark.core.exceptions.ApiException;
import xyz.juandiii.ark.core.exceptions.ConnectionException;
import xyz.juandiii.ark.core.exceptions.TimeoutException;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.http.RetryPolicy;
import xyz.juandiii.ark.core.http.Transport;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Generic retry decorator that wraps any {@link Transport} with exponential
 * backoff and jitter. Execution-model-agnostic — the per-model details live in
 * the {@link RetryOps} strategy implementation passed at construction.
 *
 * <p>Compose via {@code transport.with(Retry.of(policy, ops))} or construct
 * directly via the public constructor.
 *
 * <p>Only retries idempotent methods (GET, HEAD, PUT, DELETE, OPTIONS) by default
 * unless {@link RetryPolicy.Builder#retryPost(boolean)} is enabled.
 *
 * @param <R> the transport's return wrapper
 *
 * @author Juan Diego Lopez V.
 */
public final class Retry<R> implements Transport<R> {

    private static final Set<String> IDEMPOTENT_METHODS =
            Set.of("GET", "HEAD", "PUT", "DELETE", "OPTIONS");

    private final Transport<R> delegate;
    private final RetryPolicy policy;
    private final RetryOps<R> ops;
    private final long[] precomputedDelays;

    public Retry(Transport<R> delegate, RetryPolicy policy, RetryOps<R> ops) {
        this.delegate = delegate;
        this.policy = policy;
        this.ops = ops;
        this.precomputedDelays = precomputeDelays(policy);
    }

    /**
     * Factory returning a {@link Function} usable with {@link Transport#with}.
     * Reads as {@code transport.with(Retry.of(policy, ops))}.
     *
     * @param policy retry configuration
     * @param ops    per-model strategy
     * @param <R>    transport's return wrapper
     * @return decorator function
     */
    public static <R> Function<Transport<R>, Transport<R>> of(RetryPolicy policy, RetryOps<R> ops) {
        return delegate -> new Retry<>(delegate, policy, ops);
    }

    public static long[] precomputeDelays(RetryPolicy policy) {
        long baseDelay = policy.delay().toMillis();
        long maxDelay = policy.maxDelay().toMillis();
        double multiplier = policy.multiplier();
        int n = Math.max(policy.maxAttempts(), 1);
        long[] delays = new long[n];
        double current = baseDelay;
        for (int i = 0; i < n; i++) {
            delays[i] = Math.min((long) current, maxDelay);
            current *= multiplier;
        }
        return delays;
    }

    @Override
    public R send(String method, URI uri, Map<String, String> headers,
                  String body, Duration timeout) {
        return attempt(method, () -> delegate.send(method, uri, headers, body, timeout), 1);
    }

    @Override
    public R sendBinary(String method, URI uri, Map<String, String> headers,
                        byte[] body, Duration timeout) {
        return attempt(method, () -> delegate.sendBinary(method, uri, headers, body, timeout), 1);
    }

    private R attempt(String method, Supplier<R> nextCall, int attemptNum) {
        return ops.adapt(nextCall,
                (response, n) -> handleSuccess(method, nextCall, response, n),
                (throwable, n) -> handleError(method, nextCall, throwable, n),
                attemptNum);
    }

    private R handleSuccess(String method, Supplier<R> nextCall, RawResponse response, int n) {
        if (!response.isError()) return ops.succeeded(response);
        int statusCode = response.statusCode();
        if (n >= policy.maxAttempts() || !isRetryableStatus(method, statusCode)) {
            return ops.failed(ApiException.of(statusCode, response.body()));
        }
        long delayMs = computeDelay(n);
        return ops.delay(delayMs, () -> attempt(method, nextCall, n + 1));
    }

    private R handleError(String method, Supplier<R> nextCall, Throwable t, int n) {
        if (n >= policy.maxAttempts() || !isRetryableException(method, t)) {
            return ops.failed(t);
        }
        long delayMs = computeDelay(n);
        return ops.delay(delayMs, () -> attempt(method, nextCall, n + 1));
    }

    private boolean isRetryableStatus(String method, int statusCode) {
        return isRetryableMethod(method) && policy.retryOn().contains(statusCode);
    }

    private boolean isRetryableException(String method, Throwable cause) {
        if (!isRetryableMethod(method)) return false;
        if (cause instanceof TimeoutException || cause instanceof ConnectionException) {
            return policy.retryOnException();
        }
        return false;
    }

    private boolean isRetryableMethod(String method) {
        return IDEMPOTENT_METHODS.contains(method.toUpperCase()) || policy.retryPost();
    }

    private long computeDelay(int attempt) {
        int index = Math.min(attempt - 1, precomputedDelays.length - 1);
        long capped = precomputedDelays[index];
        long minJitter = Math.max(capped / 2, 1);
        return ThreadLocalRandom.current().nextLong(minJitter, capped + 1);
    }
}
