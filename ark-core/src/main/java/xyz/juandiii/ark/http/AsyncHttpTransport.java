package xyz.juandiii.ark.http;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface AsyncHttpTransport {

    CompletableFuture<RawResponse> sendAsync(String method, URI uri, Map<String, String> headers,
                                             String body, Duration timeout);
}