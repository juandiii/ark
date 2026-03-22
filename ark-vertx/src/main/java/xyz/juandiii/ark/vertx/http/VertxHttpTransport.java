package xyz.juandiii.ark.vertx.http;

import io.vertx.core.Future;
import xyz.juandiii.ark.http.RawResponse;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

@FunctionalInterface
public interface VertxHttpTransport {

    Future<RawResponse> send(String method, URI uri, Map<String, String> headers,
                             String body, Duration timeout);
}
