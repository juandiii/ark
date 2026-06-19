package xyz.juandiii.ark.reactor.http;

import reactor.core.publisher.Mono;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.http.Transport;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

/**
 * Functional interface for Reactor-based HTTP transport.
 * Extends {@code Transport<Mono<RawResponse>>} so decorators like
 * {@code Retry<>} compose via {@code transport.with(...)}.
 *
 * @author Juan Diego Lopez V.
 */
@FunctionalInterface
public interface ReactorHttpTransport extends Transport<Mono<RawResponse>> {

    @Override
    Mono<RawResponse> send(String method, URI uri, Map<String, String> headers,
                           String body, Duration timeout);

    @Override
    default Mono<RawResponse> sendBinary(String method, URI uri, Map<String, String> headers,
                                          byte[] body, Duration timeout) {
        return Transport.super.sendBinary(method, uri, headers, body, timeout);
    }
}
