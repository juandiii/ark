package xyz.juandiii.ark.core.http.decorator;

import xyz.juandiii.ark.core.http.RawResponse;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Strategy that adapts retry mechanics to the underlying return type R.
 * One implementation per execution model (sync, reactor, mutiny, vertx).
 *
 * <p>The {@link Retry} decorator is execution-model-agnostic; this strategy
 * encapsulates the per-model differences (try/catch vs CompletableFuture vs
 * reactive operators).
 *
 * @param <R> the transport's return wrapper
 *
 * @author Juan Diego Lopez V.
 */
public interface RetryOps<R> {

    /**
     * Run {@code attempt} and weave success / failure decisions into R.
     * For sync, this wraps try/catch around the supplier. For reactive,
     * it composes via the native operators ({@code Mono.flatMap}, etc.).
     *
     * @param attempt        produces the upstream R for this attempt
     * @param onSuccess      called with the upstream's RawResponse + attempt number; returns next R
     * @param onError        called with the upstream's Throwable + attempt number; returns next R
     * @param attemptNumber  1-based attempt counter, passed through to the handlers
     * @return R that delivers the outcome of {@code onSuccess} or {@code onError}
     */
    R adapt(Supplier<R> attempt,
            BiFunction<RawResponse, Integer, R> onSuccess,
            BiFunction<Throwable, Integer, R> onError,
            int attemptNumber);

    /**
     * Schedule a delayed retry. Returns R that produces the next attempt after {@code delayMs}.
     *
     * @param delayMs       delay before invoking the next attempt
     * @param nextAttempt   produces the upstream R for the next attempt
     * @return R delivering the next attempt's outcome
     */
    R delay(long delayMs, Supplier<R> nextAttempt);

    /**
     * Wrap a successful RawResponse into R.
     *
     * @param response the response to wrap
     * @return R wrapping {@code response}
     */
    R succeeded(RawResponse response);

    /**
     * Wrap an exception into R. For sync, throws it. For async / reactive,
     * returns a failed publisher.
     *
     * @param throwable the error to wrap
     * @return R representing the failure
     */
    R failed(Throwable throwable);
}
