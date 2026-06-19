package xyz.juandiii.ark.reactor.http.decorator;

import reactor.core.publisher.Mono;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.http.decorator.RetryOps;

import java.time.Duration;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * {@link RetryOps} for Reactor's {@code Mono<RawResponse>}.
 *
 * @author Juan Diego Lopez V.
 */
public final class ReactorRetryOps implements RetryOps<Mono<RawResponse>> {

    @Override
    public Mono<RawResponse> adapt(Supplier<Mono<RawResponse>> attempt,
                                    BiFunction<RawResponse, Integer, Mono<RawResponse>> onSuccess,
                                    BiFunction<Throwable, Integer, Mono<RawResponse>> onError,
                                    int attemptNumber) {
        return Mono.defer(attempt)
                .flatMap(r -> onSuccess.apply(r, attemptNumber))
                .onErrorResume(t -> onError.apply(t, attemptNumber));
    }

    @Override
    public Mono<RawResponse> delay(long delayMs, Supplier<Mono<RawResponse>> nextAttempt) {
        return Mono.defer(nextAttempt).delaySubscription(Duration.ofMillis(delayMs));
    }

    @Override
    public Mono<RawResponse> succeeded(RawResponse response) {
        return Mono.just(response);
    }

    @Override
    public Mono<RawResponse> failed(Throwable throwable) {
        return Mono.error(throwable);
    }
}
