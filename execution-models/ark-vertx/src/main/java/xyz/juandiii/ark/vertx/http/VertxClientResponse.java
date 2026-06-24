package xyz.juandiii.ark.vertx.http;

import io.vertx.core.Future;
import xyz.juandiii.ark.core.TypeRef;
import xyz.juandiii.ark.core.http.ArkResponse;
import xyz.juandiii.ark.core.http.RawResponse;

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

    /**
     * Returns the raw HTTP response — status code, headers, and body as a String —
     * without deserialization. Useful with {@link VertxClientRequest#noThrow()} (or
     * client-level {@code throwOnError(false)}) to inspect error bodies that
     * don't match a typed schema.
     *
     * @return Future completed with the raw response wrapper produced by the transport
     */
    Future<RawResponse> raw();
}
