package xyz.juandiii.ark.async.http;

import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.TypeRef;
import xyz.juandiii.ark.http.ArkResponse;
import xyz.juandiii.ark.http.RawResponse;

import java.util.concurrent.CompletableFuture;

/**
 * Default implementation of {@link AsyncClientResponse}.
 *
 * @author Juan Diego Lopez V.
 */
public final class DefaultAsyncClientResponse implements AsyncClientResponse {

    private final CompletableFuture<RawResponse> future;
    private final JsonSerializer serializer;

    public DefaultAsyncClientResponse(CompletableFuture<RawResponse> future, JsonSerializer serializer) {
        this.future = future;
        this.serializer = serializer;
    }

    @Override
    public <T> CompletableFuture<T> body(TypeRef<T> type) {
        return future.thenApply(raw -> serializer.deserialize(raw.body(), type));
    }

    @Override
    public <T> CompletableFuture<T> body(Class<T> type) {
        return body(TypeRef.of(type));
    }

    @Override
    public <T> CompletableFuture<ArkResponse<T>> toEntity(TypeRef<T> type) {
        return future.thenApply(raw -> {
            T body = serializer.deserialize(raw.body(), type);
            return new ArkResponse<>(raw.statusCode(), raw.headers(), body);
        });
    }

    @Override
    public <T> CompletableFuture<ArkResponse<T>> toEntity(Class<T> type) {
        return toEntity(TypeRef.of(type));
    }

    @Override
    public CompletableFuture<ArkResponse<Void>> toBodilessEntity() {
        return future.thenApply(raw ->
                new ArkResponse<>(raw.statusCode(), raw.headers(), null));
    }
}
