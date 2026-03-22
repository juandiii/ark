package xyz.juandiii.ark.transport.reactor;

import reactor.core.publisher.Mono;
import reactor.netty.ByteBufMono;
import reactor.netty.http.client.HttpClient;
import io.netty.handler.codec.http.HttpMethod;
import xyz.juandiii.ark.exceptions.ApiException;
import xyz.juandiii.ark.http.RawResponse;
import xyz.juandiii.ark.reactor.http.ReactorHttpTransport;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ArkReactorNettyTransport implements ReactorHttpTransport {

    private final HttpClient httpClient;

    public ArkReactorNettyTransport(HttpClient httpClient) {
        Objects.requireNonNull(httpClient, "HttpClient is required");
        this.httpClient = httpClient;
    }

    @Override
    public Mono<RawResponse> send(String method, URI uri, Map<String, String> headers,
                                  String body, Duration timeout) {
        Mono<RawResponse> result = httpClient
                .headers(h -> headers.forEach(h::set))
                .request(HttpMethod.valueOf(method))
                .uri(uri)
                .send(body != null ? ByteBufMono.fromString(Mono.just(body)) : Mono.empty())
                .responseSingle((response, content) ->
                        content.asString().defaultIfEmpty("").map(responseBody -> {
                            int statusCode = response.status().code();
                            if (statusCode >= 400) {
                                throw new ApiException(statusCode, responseBody);
                            }
                            return new RawResponse(statusCode, toHeaderMap(response.responseHeaders()), responseBody);
                        })
                );

        if (timeout != null) {
            result = result.timeout(timeout);
        }

        return result;
    }

    private Map<String, List<String>> toHeaderMap(io.netty.handler.codec.http.HttpHeaders nettyHeaders) {
        Map<String, List<String>> headers = new HashMap<>();
        nettyHeaders.forEach(entry ->
                headers.computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
                        .add(entry.getValue()));
        return headers;
    }
}
