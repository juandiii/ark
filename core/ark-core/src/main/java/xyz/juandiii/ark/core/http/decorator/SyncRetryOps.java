package xyz.juandiii.ark.core.http.decorator;

import xyz.juandiii.ark.core.exceptions.RequestInterruptedException;
import xyz.juandiii.ark.core.http.RawResponse;

import java.util.function.BiFunction;
import java.util.function.LongConsumer;
import java.util.function.Supplier;

/**
 * Synchronous {@link RetryOps} for {@code Transport<RawResponse>}. Uses
 * {@code Thread.sleep} (or an injected sleeper for testing) for delays and
 * try/catch for adapt.
 *
 * @author Juan Diego Lopez V.
 */
public final class SyncRetryOps implements RetryOps<RawResponse> {

    private final LongConsumer sleeper;

    public SyncRetryOps() {
        this(SyncRetryOps::threadSleep);
    }

    public SyncRetryOps(LongConsumer sleeper) {
        this.sleeper = sleeper;
    }

    @Override
    public RawResponse adapt(Supplier<RawResponse> attempt,
                              BiFunction<RawResponse, Integer, RawResponse> onSuccess,
                              BiFunction<Throwable, Integer, RawResponse> onError,
                              int attemptNumber) {
        RawResponse r;
        try {
            r = attempt.get();
        } catch (Throwable t) {
            return onError.apply(t, attemptNumber);
        }
        return onSuccess.apply(r, attemptNumber);
    }

    @Override
    public RawResponse delay(long delayMs, Supplier<RawResponse> nextAttempt) {
        sleeper.accept(delayMs);
        return nextAttempt.get();
    }

    @Override
    public RawResponse succeeded(RawResponse response) {
        return response;
    }

    @Override
    public RawResponse failed(Throwable throwable) {
        if (throwable instanceof RuntimeException re) throw re;
        if (throwable instanceof Error e) throw e;
        throw new RuntimeException(throwable);
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
