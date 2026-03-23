package xyz.juandiii.ark.reactor.http;

import reactor.core.publisher.Mono;
import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.TypeRef;
import xyz.juandiii.ark.http.ArkResponse;
import xyz.juandiii.ark.http.RawResponse;

public final class DefaultReactorClientResponse implements ReactorClientResponse {

    private final Mono<RawResponse> mono;
    private final JsonSerializer serializer;

    public DefaultReactorClientResponse(Mono<RawResponse> mono, JsonSerializer serializer) {
        this.mono = mono;
        this.serializer = serializer;
    }

    @Override
    public <T> Mono<T> body(TypeRef<T> type) {
        return mono.map(raw -> serializer.deserialize(raw.body(), type));
    }

    @Override
    public <T> Mono<T> body(Class<T> type) {
        return body(TypeRef.of(type));
    }

    @Override
    public <T> Mono<ArkResponse<T>> toEntity(TypeRef<T> type) {
        return mono.map(raw -> {
            T body = serializer.deserialize(raw.body(), type);
            return new ArkResponse<>(raw.statusCode(), raw.headers(), body);
        });
    }

    @Override
    public <T> Mono<ArkResponse<T>> toEntity(Class<T> type) {
        return toEntity(TypeRef.of(type));
    }

    @Override
    public Mono<ArkResponse<Void>> toBodilessEntity() {
        return mono.map(raw ->
                new ArkResponse<>(raw.statusCode(), raw.headers(), null));
    }
}
