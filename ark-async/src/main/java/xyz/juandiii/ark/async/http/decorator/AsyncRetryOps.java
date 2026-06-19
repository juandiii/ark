package xyz.juandiii.ark.async.http.decorator;

import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.http.decorator.RetryOps;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * {@link RetryOps} for {@code CompletableFuture<RawResponse>}. Pairs with
 * {@link Adapters#fromAsync} as the escape hatch for async users who want
 * the {@code transport.with(Retry.of(...))} ergonomics.
 *
 * <p>For the canonical async retry path, use
 * {@link xyz.juandiii.ark.async.http.RetryAsyncTransport} directly.
 *
 * @author Juan Diego Lopez V.
 */
public final class AsyncRetryOps implements RetryOps<CompletableFuture<RawResponse>> {

    private final ScheduledExecutorService scheduler;

    public AsyncRetryOps() {
        this(Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ark-async-retry");
            t.setDaemon(true);
            return t;
        }));
    }

    public AsyncRetryOps(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public CompletableFuture<RawResponse> adapt(Supplier<CompletableFuture<RawResponse>> attempt,
                                                 BiFunction<RawResponse, Integer, CompletableFuture<RawResponse>> onSuccess,
                                                 BiFunction<Throwable, Integer, CompletableFuture<RawResponse>> onError,
                                                 int attemptNumber) {
        return attempt.get().handle((r, t) -> {
            if (t != null) return onError.apply(unwrap(t), attemptNumber);
            return onSuccess.apply(r, attemptNumber);
        }).thenCompose(f -> f);
    }

    @Override
    public CompletableFuture<RawResponse> delay(long delayMs, Supplier<CompletableFuture<RawResponse>> nextAttempt) {
        CompletableFuture<RawResponse> delayed = new CompletableFuture<>();
        scheduler.schedule(() -> nextAttempt.get().whenComplete((r, e) -> {
            if (e != null) delayed.completeExceptionally(unwrap(e));
            else delayed.complete(r);
        }), delayMs, TimeUnit.MILLISECONDS);
        return delayed;
    }

    @Override
    public CompletableFuture<RawResponse> succeeded(RawResponse response) {
        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<RawResponse> failed(Throwable throwable) {
        return CompletableFuture.failedFuture(throwable);
    }

    private static Throwable unwrap(Throwable t) {
        return t instanceof CompletionException && t.getCause() != null ? t.getCause() : t;
    }
}
