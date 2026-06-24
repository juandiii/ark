package xyz.juandiii.ark.async.http;

import xyz.juandiii.ark.core.TypeRef;
import xyz.juandiii.ark.core.http.ArkResponse;
import xyz.juandiii.ark.core.http.RawResponse;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for async response extraction.
 *
 * @author Juan Diego Lopez V.
 */
public interface AsyncClientResponse {

    <T> CompletableFuture<T> body(TypeRef<T> type);

    <T> CompletableFuture<T> body(Class<T> type);

    <T> CompletableFuture<ArkResponse<T>> toEntity(TypeRef<T> type);

    <T> CompletableFuture<ArkResponse<T>> toEntity(Class<T> type);

    CompletableFuture<ArkResponse<Void>> toBodilessEntity();

    /**
     * Returns the raw HTTP response — status code, headers, and body as a String —
     * without deserialization. Useful with {@link AsyncClientRequest#noThrow()} (or
     * client-level {@code throwOnError(false)}) to inspect error bodies that
     * don't match a typed schema.
     *
     * @return future completed with the raw response wrapper produced by the transport
     */
    CompletableFuture<RawResponse> raw();
}
