package xyz.juandiii.ark.vertx.http;

import io.vertx.core.Future;
import xyz.juandiii.ark.core.TypeRef;
import xyz.juandiii.ark.core.http.ArkResponse;

/**
 * Interface for Vert.x response extraction.
 *
 * @author Juan Diego Lopez V.
 */
public interface VertxClientResponse {

    <T> Future<T> body(TypeRef<T> type);

    <T> Future<T> body(Class<T> type);

    <T> Future<ArkResponse<T>> toEntity(TypeRef<T> type);

    <T> Future<ArkResponse<T>> toEntity(Class<T> type);

    Future<ArkResponse<Void>> toBodilessEntity();
}
