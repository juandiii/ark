package xyz.juandiii.ark.async.http;

import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.TypeRef;
import xyz.juandiii.ark.http.ArkResponse;
import xyz.juandiii.ark.http.RawResponse;

import java.util.concurrent.CompletableFuture;

public final class AsyncResponseSpec {

    private final CompletableFuture<RawResponse> future;
    private final JsonSerializer serializer;

    public AsyncResponseSpec(CompletableFuture<RawResponse> future, JsonSerializer serializer) {
        this.future = future;
        this.serializer = serializer;
    }

    public <T> CompletableFuture<T> body(TypeRef<T> type) {
        return future.thenApply(raw -> serializer.deserialize(raw.body(), type));
    }

    public <T> CompletableFuture<ArkResponse<T>> toEntity(TypeRef<T> type) {
        return future.thenApply(raw -> {
            T body = serializer.deserialize(raw.body(), type);
            return new ArkResponse<>(raw.statusCode(), raw.headers(), body);
        });
    }

    public CompletableFuture<ArkResponse<Void>> toBodilessEntity() {
        return future.thenApply(raw ->
                new ArkResponse<>(raw.statusCode(), raw.headers(), null));
    }
}
