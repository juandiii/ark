package xyz.juandiii.ark.mutiny.http;

import io.smallrye.mutiny.Uni;
import xyz.juandiii.ark.http.RawResponse;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

/**
 * Functional interface for Mutiny-based HTTP transport.
 *
 * @author Juan Diego Lopez V.
 */
@FunctionalInterface
public interface MutinyHttpTransport {

    Uni<RawResponse> send(String method, URI uri, Map<String, String> headers,
                          String body, Duration timeout);
}
