package xyz.juandiii.ark.mutiny.http;

import io.smallrye.mutiny.Uni;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.http.Transport;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

/**
 * Functional interface for Mutiny-based HTTP transport.
 * Extends {@code Transport<Uni<RawResponse>>} so decorators like
 * {@code Retry<>} compose via {@code transport.with(...)}.
 *
 * @author Juan Diego Lopez V.
 */
@FunctionalInterface
public interface MutinyHttpTransport extends Transport<Uni<RawResponse>> {

    @Override
    Uni<RawResponse> send(String method, URI uri, Map<String, String> headers,
                          String body, Duration timeout);

    @Override
    default Uni<RawResponse> sendBinary(String method, URI uri, Map<String, String> headers,
                                         byte[] body, Duration timeout) {
        return Transport.super.sendBinary(method, uri, headers, body, timeout);
    }
}
