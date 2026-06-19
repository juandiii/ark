package xyz.juandiii.ark.mutiny.http.decorator;

import io.smallrye.mutiny.Uni;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.http.decorator.RetryOps;

import java.time.Duration;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * {@link RetryOps} for Mutiny's {@code Uni<RawResponse>}.
 *
 * @author Juan Diego Lopez V.
 */
public final class MutinyRetryOps implements RetryOps<Uni<RawResponse>> {

    @Override
    public Uni<RawResponse> adapt(Supplier<Uni<RawResponse>> attempt,
                                   BiFunction<RawResponse, Integer, Uni<RawResponse>> onSuccess,
                                   BiFunction<Throwable, Integer, Uni<RawResponse>> onError,
                                   int attemptNumber) {
        return Uni.createFrom().deferred(attempt::get)
                .onItem().transformToUni(r -> onSuccess.apply(r, attemptNumber))
                .onFailure().recoverWithUni(t -> onError.apply(t, attemptNumber));
    }

    @Override
    public Uni<RawResponse> delay(long delayMs, Supplier<Uni<RawResponse>> nextAttempt) {
        return Uni.createFrom().deferred(nextAttempt::get)
                .onItem().delayIt().by(Duration.ofMillis(delayMs));
    }

    @Override
    public Uni<RawResponse> succeeded(RawResponse response) {
        return Uni.createFrom().item(response);
    }

    @Override
    public Uni<RawResponse> failed(Throwable throwable) {
        return Uni.createFrom().failure(throwable);
    }
}
