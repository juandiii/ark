package xyz.juandiii.ark.reactor.http;

import reactor.core.publisher.Mono;
import xyz.juandiii.ark.TypeRef;
import xyz.juandiii.ark.http.ArkResponse;

public interface ReactorClientResponse {

    <T> Mono<T> body(TypeRef<T> type);

    <T> Mono<T> body(Class<T> type);

    <T> Mono<ArkResponse<T>> toEntity(TypeRef<T> type);

    <T> Mono<ArkResponse<T>> toEntity(Class<T> type);

    Mono<ArkResponse<Void>> toBodilessEntity();
}
