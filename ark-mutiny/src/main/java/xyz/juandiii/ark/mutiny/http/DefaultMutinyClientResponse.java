package xyz.juandiii.ark.mutiny.http;

import io.smallrye.mutiny.Uni;
import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.TypeRef;
import xyz.juandiii.ark.http.ArkResponse;
import xyz.juandiii.ark.http.RawResponse;

public final class DefaultMutinyClientResponse implements MutinyClientResponse {

    private final Uni<RawResponse> uni;
    private final JsonSerializer serializer;

    public DefaultMutinyClientResponse(Uni<RawResponse> uni, JsonSerializer serializer) {
        this.uni = uni;
        this.serializer = serializer;
    }

    @Override
    public <T> Uni<T> body(TypeRef<T> type) {
        return uni.onItem().transform(raw -> serializer.deserialize(raw.body(), type));
    }

    @Override
    public <T> Uni<T> body(Class<T> type) {
        return body(TypeRef.of(type));
    }

    @Override
    public <T> Uni<ArkResponse<T>> toEntity(TypeRef<T> type) {
        return uni.onItem().transform(raw -> {
            T body = serializer.deserialize(raw.body(), type);
            return new ArkResponse<>(raw.statusCode(), raw.headers(), body);
        });
    }

    @Override
    public <T> Uni<ArkResponse<T>> toEntity(Class<T> type) {
        return toEntity(TypeRef.of(type));
    }

    @Override
    public Uni<ArkResponse<Void>> toBodilessEntity() {
        return uni.onItem().transform(raw ->
                new ArkResponse<>(raw.statusCode(), raw.headers(), null));
    }
}
