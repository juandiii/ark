package xyz.juandiii.ark.async.http;

import xyz.juandiii.ark.core.TypeRef;
import xyz.juandiii.ark.core.http.ArkResponse;

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
}
