package xyz.juandiii.ark.async.http;

import xyz.juandiii.ark.TypeRef;
import xyz.juandiii.ark.http.ArkResponse;

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
