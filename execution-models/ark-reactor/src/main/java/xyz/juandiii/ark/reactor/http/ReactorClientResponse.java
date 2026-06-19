package xyz.juandiii.ark.reactor.http;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.juandiii.ark.core.TypeRef;
import xyz.juandiii.ark.core.http.ArkResponse;

import java.util.List;

/**
 * Interface for reactive response extraction.
 *
 * @author Juan Diego Lopez V.
 */
public interface ReactorClientResponse {

    <T> Mono<T> body(TypeRef<T> type);

    <T> Mono<T> body(Class<T> type);

    @SuppressWarnings("unchecked")
    default <T> Flux<T> bodyAsFlux(TypeRef<T> type) {
        return ((Mono<List<T>>) (Mono<?>) body(TypeRef.ofList(type)))
                .flatMapMany(Flux::fromIterable);
    }

    @SuppressWarnings("unchecked")
    default <T> Flux<T> bodyAsFlux(Class<T> type) {
        return ((Mono<List<T>>) (Mono<?>) body(TypeRef.ofList(type)))
                .flatMapMany(Flux::fromIterable);
    }

    <T> Mono<ArkResponse<T>> toEntity(TypeRef<T> type);

    <T> Mono<ArkResponse<T>> toEntity(Class<T> type);

    Mono<ArkResponse<Void>> toBodilessEntity();
}
