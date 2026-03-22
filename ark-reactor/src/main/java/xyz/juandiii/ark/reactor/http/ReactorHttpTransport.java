package xyz.juandiii.ark.reactor.http;

import reactor.core.publisher.Mono;
import xyz.juandiii.ark.http.RawResponse;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

@FunctionalInterface
public interface ReactorHttpTransport {

    Mono<RawResponse> send(String method, URI uri, Map<String, String> headers,
                           String body, Duration timeout);
}
