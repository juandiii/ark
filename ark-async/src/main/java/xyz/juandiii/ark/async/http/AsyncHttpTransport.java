package xyz.juandiii.ark.async.http;

import xyz.juandiii.ark.core.http.RawResponse;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Functional interface for asynchronous HTTP transport.
 *
 * @author Juan Diego Lopez V.
 */
@FunctionalInterface
public interface AsyncHttpTransport {

    CompletableFuture<RawResponse> sendAsync(String method, URI uri, Map<String, String> headers,
                                             String body, Duration timeout);
}
