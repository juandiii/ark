package xyz.juandiii.ark.reactor.http;

import reactor.core.publisher.Mono;
import xyz.juandiii.ark.TypeRef;
import xyz.juandiii.ark.http.ArkResponse;

/**
 * Interface for reactive response extraction.
 *
 * @author Juan Diego Lopez V.
 */
public interface ReactorClientResponse {

    <T> Mono<T> body(TypeRef<T> type);

    <T> Mono<T> body(Class<T> type);

    <T> Mono<ArkResponse<T>> toEntity(TypeRef<T> type);

    <T> Mono<ArkResponse<T>> toEntity(Class<T> type);

    Mono<ArkResponse<Void>> toBodilessEntity();
}
