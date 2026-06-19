package xyz.juandiii.ark.vertx.http.decorator;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.http.decorator.RetryOps;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * {@link RetryOps} for Vert.x {@code Future<RawResponse>}. Uses a small
 * daemon {@link ScheduledExecutorService} for delays — Vert.x doesn't expose
 * a context-agnostic scheduler from {@code Future} alone, so we maintain our
 * own (mirroring {@code RetryAsyncTransport}'s approach in ark-async).
 *
 * @author Juan Diego Lopez V.
 */
public final class VertxRetryOps implements RetryOps<Future<RawResponse>> {

    private final ScheduledExecutorService scheduler;

    public VertxRetryOps() {
        this(Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ark-vertx-retry");
            t.setDaemon(true);
            return t;
        }));
    }

    public VertxRetryOps(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public Future<RawResponse> adapt(Supplier<Future<RawResponse>> attempt,
                                      BiFunction<RawResponse, Integer, Future<RawResponse>> onSuccess,
                                      BiFunction<Throwable, Integer, Future<RawResponse>> onError,
                                      int attemptNumber) {
        Promise<RawResponse> promise = Promise.promise();
        attempt.get()
                .onSuccess(r -> onSuccess.apply(r, attemptNumber)
                        .onSuccess(promise::complete).onFailure(promise::fail))
                .onFailure(t -> onError.apply(t, attemptNumber)
                        .onSuccess(promise::complete).onFailure(promise::fail));
        return promise.future();
    }

    @Override
    public Future<RawResponse> delay(long delayMs, Supplier<Future<RawResponse>> nextAttempt) {
        Promise<RawResponse> promise = Promise.promise();
        scheduler.schedule(() -> nextAttempt.get()
                        .onSuccess(promise::complete).onFailure(promise::fail),
                delayMs, TimeUnit.MILLISECONDS);
        return promise.future();
    }

    @Override
    public Future<RawResponse> succeeded(RawResponse response) {
        return Future.succeededFuture(response);
    }

    @Override
    public Future<RawResponse> failed(Throwable throwable) {
        return Future.failedFuture(throwable);
    }
}
