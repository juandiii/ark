package xyz.juandiii.ark.http;

import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.TypeRef;

public final class ResponseSpec {

    private final RawResponse raw;
    private final JsonSerializer serializer;

    ResponseSpec(RawResponse raw, JsonSerializer serializer) {
        this.raw = raw;
        this.serializer = serializer;
    }

    public <T> T body(TypeRef<T> type) {
        return serializer.deserialize(raw.body(), type);
    }

    public <T> T body(Class<T> type) {
        return serializer.deserialize(raw.body(), TypeRef.of(type));
    }

    public <T> ArkResponse<T> toEntity(TypeRef<T> type) {
        T body = serializer.deserialize(raw.body(), type);
        return new ArkResponse<>(raw.statusCode(), raw.headers(), body);
    }

    public <T> ArkResponse<T> toEntity(Class<T> type) {
        return toEntity(TypeRef.of(type));
    }

    public ArkResponse<Void> toBodilessEntity() {
        return new ArkResponse<>(raw.statusCode(), raw.headers(), null);
    }
}
