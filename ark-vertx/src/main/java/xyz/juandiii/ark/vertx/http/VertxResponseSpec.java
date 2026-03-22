package xyz.juandiii.ark.vertx.http;

import io.vertx.core.Future;
import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.TypeRef;
import xyz.juandiii.ark.http.ArkResponse;
import xyz.juandiii.ark.http.RawResponse;

public final class VertxResponseSpec {

    private final Future<RawResponse> future;
    private final JsonSerializer serializer;

    public VertxResponseSpec(Future<RawResponse> future, JsonSerializer serializer) {
        this.future = future;
        this.serializer = serializer;
    }

    public <T> Future<T> body(TypeRef<T> type) {
        return future.map(raw -> serializer.deserialize(raw.body(), type));
    }

    public <T> Future<T> body(Class<T> type) {
        return body(TypeRef.of(type));
    }

    public <T> Future<ArkResponse<T>> toEntity(TypeRef<T> type) {
        return future.map(raw -> {
            T body = serializer.deserialize(raw.body(), type);
            return new ArkResponse<>(raw.statusCode(), raw.headers(), body);
        });
    }

    public <T> Future<ArkResponse<T>> toEntity(Class<T> type) {
        return toEntity(TypeRef.of(type));
    }

    public Future<ArkResponse<Void>> toBodilessEntity() {
        return future.map(raw ->
                new ArkResponse<>(raw.statusCode(), raw.headers(), null));
    }
}
