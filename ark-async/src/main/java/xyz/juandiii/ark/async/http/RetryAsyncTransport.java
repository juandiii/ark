package xyz.juandiii.ark.async.http;

import xyz.juandiii.ark.core.exceptions.ApiException;
import xyz.juandiii.ark.core.exceptions.ConnectionException;
import xyz.juandiii.ark.core.exceptions.TimeoutException;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.http.RetryPolicy;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Async transport decorator that retries failed requests with exponential backoff and jitter.
 *
 * @author Juan Diego Lopez V.
 */
public final class RetryAsyncTransport implements AsyncHttpTransport {

    private static final System.Logger LOGGER = System.getLogger("xyz.juandiii.ark.retry");
    private static final Set<String> IDEMPOTENT_METHODS = Set.of("GET", "HEAD", "PUT", "DELETE", "OPTIONS");

    private final AsyncHttpTransport delegate;
    private final RetryPolicy policy;
    private final ScheduledExecutorService scheduler;

    public RetryAsyncTransport(AsyncHttpTransport delegate, RetryPolicy policy) {
        this(delegate, policy, Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ark-retry-scheduler");
            t.setDaemon(true);
            return t;
        }));
    }

    public RetryAsyncTransport(AsyncHttpTransport delegate, RetryPolicy policy,
                               ScheduledExecutorService scheduler) {
        this.delegate = delegate;
        this.policy = policy;
        this.scheduler = scheduler;
    }

    @Override
    public CompletableFuture<RawResponse> sendAsync(String method, URI uri,
                                                     Map<String, String> headers,
                                                     String body, Duration timeout) {
        return attemptAsync(method, uri, headers, body, timeout, 1);
    }

    private CompletableFuture<RawResponse> attemptAsync(String method, URI uri,
                                                         Map<String, String> headers,
                                                         String body, Duration timeout,
                                                         int attempt) {
        return delegate.sendAsync(method, uri, headers, body, timeout)
                .exceptionallyCompose(throwable -> {
                    Throwable cause = unwrap(throwable);

                    if (attempt >= policy.maxAttempts() || !isRetryable(method, cause)) {
                        return CompletableFuture.failedFuture(cause);
                    }

                    long delayMs = computeDelayMillis(attempt);
                    logRetry(attempt, method, uri, cause, delayMs);

                    CompletableFuture<RawResponse> delayed = new CompletableFuture<>();
                    scheduler.schedule(
                            () -> attemptAsync(method, uri, headers, body, timeout, attempt + 1)
                                    .whenComplete((r, e) -> {
                                        if (e != null) delayed.completeExceptionally(unwrap(e));
                                        else delayed.complete(r);
                                    }),
                            delayMs, TimeUnit.MILLISECONDS
                    );
                    return delayed;
                });
    }

    private boolean isRetryable(String method, Throwable cause) {
        if (!isRetryableMethod(method)) return false;
        if (cause instanceof ApiException api) {
            return policy.retryOn().contains(api.statusCode());
        }
        if (cause instanceof TimeoutException || cause instanceof ConnectionException) {
            return policy.retryOnException();
        }
        return false;
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

    private void logRetry(int attempt, String method, URI uri, Throwable cause, long delayMs) {
        String detail = cause instanceof ApiException api
                ? "(HTTP " + api.statusCode() + ")"
                : "(exception)";
        LOGGER.log(System.Logger.Level.WARNING,
                "Retry {0}/{1} for {2} {3} {4} - waiting {5}ms",
                attempt, policy.maxAttempts(), method, uri, detail, delayMs);
    }

    private static Throwable unwrap(Throwable t) {
        return t instanceof CompletionException && t.getCause() != null ? t.getCause() : t;
    }
}
