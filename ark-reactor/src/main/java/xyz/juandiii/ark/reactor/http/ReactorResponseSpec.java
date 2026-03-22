package xyz.juandiii.ark.reactor.http;

import reactor.core.publisher.Mono;
import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.TypeRef;
import xyz.juandiii.ark.http.ArkResponse;
import xyz.juandiii.ark.http.RawResponse;

public final class ReactorResponseSpec {

    private final Mono<RawResponse> mono;
    private final JsonSerializer serializer;

    public ReactorResponseSpec(Mono<RawResponse> mono, JsonSerializer serializer) {
        this.mono = mono;
        this.serializer = serializer;
    }

    public <T> Mono<T> body(TypeRef<T> type) {
        return mono.map(raw -> serializer.deserialize(raw.body(), type));
    }

    public <T> Mono<ArkResponse<T>> toEntity(TypeRef<T> type) {
        return mono.map(raw -> {
            T body = serializer.deserialize(raw.body(), type);
            return new ArkResponse<>(raw.statusCode(), raw.headers(), body);
        });
    }

    public Mono<ArkResponse<Void>> toBodilessEntity() {
        return mono.map(raw ->
                new ArkResponse<>(raw.statusCode(), raw.headers(), null));
    }
}
