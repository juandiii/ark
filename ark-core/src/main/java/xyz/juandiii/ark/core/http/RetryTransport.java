package xyz.juandiii.ark.core.http;

import xyz.juandiii.ark.core.exceptions.ApiException;
import xyz.juandiii.ark.core.exceptions.ConnectionException;
import xyz.juandiii.ark.core.exceptions.RequestInterruptedException;
import xyz.juandiii.ark.core.exceptions.TimeoutException;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongConsumer;

/**
 * Transport decorator that retries failed requests with exponential backoff and jitter.
 * Only retries idempotent methods (GET, HEAD, PUT, DELETE, OPTIONS) unless {@code retryPost} is enabled.
 *
 * @author Juan Diego Lopez V.
 */
public final class RetryTransport implements HttpTransport {

    private static final System.Logger LOGGER = System.getLogger("xyz.juandiii.ark.retry");
    private static final Set<String> IDEMPOTENT_METHODS = Set.of("GET", "HEAD", "PUT", "DELETE", "OPTIONS");

    private final HttpTransport delegate;
    private final RetryPolicy policy;
    private final LongConsumer sleeper;

    public RetryTransport(HttpTransport delegate, RetryPolicy policy) {
        this(delegate, policy, RetryTransport::threadSleep);
    }

    RetryTransport(HttpTransport delegate, RetryPolicy policy, LongConsumer sleeper) {
        this.delegate = delegate;
        this.policy = policy;
        this.sleeper = sleeper;
    }

    @Override
    public RawResponse send(String method, URI uri, Map<String, String> headers,
                            String body, Duration timeout) {
        for (int attempt = 1; attempt <= policy.maxAttempts(); attempt++) {
            try {
                return delegate.send(method, uri, headers, body, timeout);
            } catch (ApiException e) {
                if (attempt == policy.maxAttempts()) {
                    logExhausted(attempt, method, uri, e.statusCode());
                    throw e;
                }
                if (!policy.retryOn().contains(e.statusCode()) || !isRetryableMethod(method)) {
                    throw e;
                }
                long delayMs = computeDelayMillis(attempt);
                logRetry(attempt, method, uri, e.statusCode(), delayMs);
                sleeper.accept(delayMs);
            } catch (TimeoutException | ConnectionException e) {
                if (attempt == policy.maxAttempts()) {
                    logExhausted(attempt, method, uri, -1);
                    throw e;
                }
                if (!policy.retryOnException() || !isRetryableMethod(method)) {
                    throw e;
                }
                long delayMs = computeDelayMillis(attempt);
                logRetry(attempt, method, uri, -1, delayMs);
                sleeper.accept(delayMs);
            }
        }
        throw new IllegalStateException("Retry loop completed without result");
    }

    private boolean isRetryableMethod(String method) {
        return IDEMPOTENT_METHODS.contains(method.toUpperCase()) || policy.retryPost();
    }

    private long computeDelayMillis(int attempt) {
        long baseDelay = policy.delay().toMillis();
        long exponentialDelay = (long) (baseDelay * Math.pow(policy.multiplier(), attempt - 1));
        long cappedDelay = Math.min(exponentialDelay, policy.maxDelay().toMillis());
        long minJitter = Math.max(cappedDelay / 2, 1);
        return ThreadLocalRandom.current().nextLong(minJitter, cappedDelay + 1);
    }

    private void logExhausted(int attempt, String method, URI uri, int statusCode) {
        LOGGER.log(System.Logger.Level.ERROR,
                "Retry exhausted {0}/{1} for {2} {3}{4} — giving up",
                attempt, policy.maxAttempts(), method, uri,
                statusCode > 0 ? " (HTTP " + statusCode + ")" : " (exception)");
    }

    private void logRetry(int attempt, String method, URI uri, int statusCode, long delayMs) {
        LOGGER.log(System.Logger.Level.WARNING,
                "Retry {0}/{1} for {2} {3}{4} — waiting {5}ms",
                attempt, policy.maxAttempts(), method, uri,
                statusCode > 0 ? " (HTTP " + statusCode + ")" : " (exception)",
                delayMs);
    }

    private static void threadSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RequestInterruptedException("Retry sleep interrupted", e);
        }
    }
}
