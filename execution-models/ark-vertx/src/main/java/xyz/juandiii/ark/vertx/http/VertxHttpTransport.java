package xyz.juandiii.ark.vertx.http;

import io.vertx.core.Future;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.http.Transport;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

/**
 * Functional interface for Vert.x Future-based HTTP transport.
 * Extends {@code Transport<Future<RawResponse>>} so decorators like
 * {@code Retry<>} compose via {@code transport.with(...)}.
 *
 * @author Juan Diego Lopez V.
 */
@FunctionalInterface
public interface VertxHttpTransport extends Transport<Future<RawResponse>> {

    @Override
    Future<RawResponse> send(String method, URI uri, Map<String, String> headers,
                             String body, Duration timeout);

    @Override
    default Future<RawResponse> sendBinary(String method, URI uri, Map<String, String> headers,
                                            byte[] body, Duration timeout) {
        return Transport.super.sendBinary(method, uri, headers, body, timeout);
    }
}
