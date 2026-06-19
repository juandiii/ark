package xyz.juandiii.ark.reactor.http;

import reactor.core.publisher.Mono;
import xyz.juandiii.ark.core.http.RawResponse;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

/**
 * Functional interface for Reactor-based HTTP transport.
 *
 * @author Juan Diego Lopez V.
 */
@FunctionalInterface
public interface ReactorHttpTransport {

    Mono<RawResponse> send(String method, URI uri, Map<String, String> headers,
                           String body, Duration timeout);

    default Mono<RawResponse> sendBinary(String method, URI uri, Map<String, String> headers,
                                          byte[] body, Duration timeout) {
        throw new UnsupportedOperationException(
                "Transport must override sendBinary to preserve binary fidelity. " +
                "The default lossy implementation has been removed to prevent silent corruption.");
    }
}
