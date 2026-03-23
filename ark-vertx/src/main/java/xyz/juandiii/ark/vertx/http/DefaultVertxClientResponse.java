package xyz.juandiii.ark.vertx.http;

import io.vertx.core.Future;
import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.TypeRef;
import xyz.juandiii.ark.http.ArkResponse;
import xyz.juandiii.ark.http.RawResponse;

/**
 * Default implementation of {@link VertxClientResponse}.
 *
 * @author Juan Diego Lopez V.
 */
public final class DefaultVertxClientResponse implements VertxClientResponse {

    private final Future<RawResponse> future;
    private final JsonSerializer serializer;

    public DefaultVertxClientResponse(Future<RawResponse> future, JsonSerializer serializer) {
        this.future = future;
        this.serializer = serializer;
    }

    @Override
    public <T> Future<T> body(TypeRef<T> type) {
        return future.map(raw -> serializer.deserialize(raw.body(), type));
    }

    @Override
    public <T> Future<T> body(Class<T> type) {
        return body(TypeRef.of(type));
    }

    @Override
    public <T> Future<ArkResponse<T>> toEntity(TypeRef<T> type) {
        return future.map(raw -> {
            T body = serializer.deserialize(raw.body(), type);
            return new ArkResponse<>(raw.statusCode(), raw.headers(), body);
        });
    }

    @Override
    public <T> Future<ArkResponse<T>> toEntity(Class<T> type) {
        return toEntity(TypeRef.of(type));
    }

    @Override
    public Future<ArkResponse<Void>> toBodilessEntity() {
        return future.map(raw ->
                new ArkResponse<>(raw.statusCode(), raw.headers(), null));
    }
}
