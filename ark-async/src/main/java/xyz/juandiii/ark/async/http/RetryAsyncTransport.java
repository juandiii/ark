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
import java.util.function.Supplier;

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
    private final long[] precomputedDelays;

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
        this.precomputedDelays = precomputeDelays(policy);
    }

    static long[] precomputeDelays(RetryPolicy policy) {
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
    public CompletableFuture<RawResponse> sendAsync(String method, URI uri,
                                                     Map<String, String> headers,
                                                     String body, Duration timeout) {
        return attemptAsync(method, uri, headers, body, timeout, 1);
    }

    @Override
    public CompletableFuture<RawResponse> sendBinaryAsync(String method, URI uri,
                                                           Map<String, String> headers,
                                                           byte[] body, Duration timeout) {
        return attemptBinaryAsync(method, uri, headers, body, timeout, 1);
    }

    private CompletableFuture<RawResponse> attemptAsync(String method, URI uri,
                                                         Map<String, String> headers,
                                                         String body, Duration timeout,
                                                         int attempt) {
        return delegate.sendAsync(method, uri, headers, body, timeout)
                .handle((response, throwable) -> decideNext(method, uri, attempt, response, throwable,
                        () -> attemptAsync(method, uri, headers, body, timeout, attempt + 1)))
                .thenCompose(f -> f);
    }

    private CompletableFuture<RawResponse> attemptBinaryAsync(String method, URI uri,
                                                               Map<String, String> headers,
                                                               byte[] body, Duration timeout,
                                                               int attempt) {
        return delegate.sendBinaryAsync(method, uri, headers, body, timeout)
                .handle((response, throwable) -> decideNext(method, uri, attempt, response, throwable,
                        () -> attemptBinaryAsync(method, uri, headers, body, timeout, attempt + 1)))
                .thenCompose(f -> f);
    }

    private CompletableFuture<RawResponse> decideNext(String method, URI uri, int attempt,
                                                       RawResponse response, Throwable throwable,
                                                       Supplier<CompletableFuture<RawResponse>> nextAttempt) {
        if (throwable != null) {
            Throwable cause = unwrap(throwable);
            if (attempt >= policy.maxAttempts() || !isRetryableException(method, cause)) {
                return CompletableFuture.failedFuture(cause);
            }
            long delayMs = computeDelayMillis(attempt);
            logRetry(attempt, method, uri, cause, delayMs);
            return scheduleRetry(nextAttempt, delayMs);
        }
        if (!response.isError()) {
            return CompletableFuture.completedFuture(response);
        }
        int statusCode = response.statusCode();
        if (attempt >= policy.maxAttempts() || !isRetryableStatus(method, statusCode)) {
            return CompletableFuture.failedFuture(ApiException.of(statusCode, response.body()));
        }
        long delayMs = computeDelayMillis(attempt);
        logRetryStatus(attempt, method, uri, statusCode, delayMs);
        return scheduleRetry(nextAttempt, delayMs);
    }

    private CompletableFuture<RawResponse> scheduleRetry(Supplier<CompletableFuture<RawResponse>> nextAttempt,
                                                          long delayMs) {
        CompletableFuture<RawResponse> delayed = new CompletableFuture<>();
        scheduler.schedule(
                () -> nextAttempt.get().whenComplete((r, e) -> {
                    if (e != null) delayed.completeExceptionally(unwrap(e));
                    else delayed.complete(r);
                }),
                delayMs, TimeUnit.MILLISECONDS
        );
        return delayed;
    }

    private boolean isRetryableException(String method, Throwable cause) {
        if (!isRetryableMethod(method)) return false;
        if (cause instanceof TimeoutException || cause instanceof ConnectionException) {
            return policy.retryOnException();
        }
        return false;
    }

    private boolean isRetryableStatus(String method, int statusCode) {
        return isRetryableMethod(method) && policy.retryOn().contains(statusCode);
    }

    private boolean isRetryableMethod(String method) {
        return IDEMPOTENT_METHODS.contains(method.toUpperCase()) || policy.retryPost();
    }

    private long computeDelayMillis(int attempt) {
        int index = Math.min(attempt - 1, precomputedDelays.length - 1);
        long cappedDelay = precomputedDelays[index];
        long minJitter = Math.max(cappedDelay / 2, 1);
        return ThreadLocalRandom.current().nextLong(minJitter, cappedDelay + 1);
    }

    private void logRetry(int attempt, String method, URI uri, Throwable cause, long delayMs) {
        LOGGER.log(System.Logger.Level.WARNING,
                "Retry {0}/{1} for {2} {3} (exception: {4}) - waiting {5}ms",
                attempt, policy.maxAttempts(), method, uri, cause.getClass().getSimpleName(), delayMs);
    }

    private void logRetryStatus(int attempt, String method, URI uri, int statusCode, long delayMs) {
        LOGGER.log(System.Logger.Level.WARNING,
                "Retry {0}/{1} for {2} {3} (HTTP {4}) - waiting {5}ms",
                attempt, policy.maxAttempts(), method, uri, statusCode, delayMs);
    }

    private static Throwable unwrap(Throwable t) {
        return t instanceof CompletionException && t.getCause() != null ? t.getCause() : t;
    }
}
