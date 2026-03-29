package xyz.juandiii.ark.vertx.http;

import io.vertx.core.Future;
import xyz.juandiii.ark.core.http.RawResponse;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

/**
 * Functional interface for Vert.x Future-based HTTP transport.
 *
 * @author Juan Diego Lopez V.
 */
@FunctionalInterface
public interface VertxHttpTransport {

    Future<RawResponse> send(String method, URI uri, Map<String, String> headers,
                             String body, Duration timeout);
}
