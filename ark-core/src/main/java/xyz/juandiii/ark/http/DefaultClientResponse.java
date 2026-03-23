package xyz.juandiii.ark.http;

import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.TypeRef;

/**
 * Default implementation of {@link ClientResponse}.
 *
 * @author Juan Diego Lopez V.
 */
public final class DefaultClientResponse implements ClientResponse {

    private final RawResponse raw;
    private final JsonSerializer serializer;

    DefaultClientResponse(RawResponse raw, JsonSerializer serializer) {
        this.raw = raw;
        this.serializer = serializer;
    }

    @Override
    public <T> T body(TypeRef<T> type) {
        return serializer.deserialize(raw.body(), type);
    }

    @Override
    public <T> T body(Class<T> type) {
        return serializer.deserialize(raw.body(), TypeRef.of(type));
    }

    @Override
    public <T> ArkResponse<T> toEntity(TypeRef<T> type) {
        T body = serializer.deserialize(raw.body(), type);
        return new ArkResponse<>(raw.statusCode(), raw.headers(), body);
    }

    @Override
    public <T> ArkResponse<T> toEntity(Class<T> type) {
        return toEntity(TypeRef.of(type));
    }

    @Override
    public ArkResponse<Void> toBodilessEntity() {
        return new ArkResponse<>(raw.statusCode(), raw.headers(), null);
    }
}
