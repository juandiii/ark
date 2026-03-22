package xyz.juandiii.ark.transport.vertx;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import xyz.juandiii.ark.async.http.AsyncHttpTransport;
import xyz.juandiii.ark.exceptions.ApiException;
import xyz.juandiii.ark.exceptions.ArkException;
import xyz.juandiii.ark.http.RawResponse;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class ArkVertxTransport implements AsyncHttpTransport {

    private final WebClient webClient;

    public ArkVertxTransport(WebClient webClient) {
        Objects.requireNonNull(webClient, "WebClient is required");
        this.webClient = webClient;
    }

    @Override
    public CompletableFuture<RawResponse> sendAsync(String method, URI uri,
                                                    Map<String, String> headers,
                                                    String body, Duration timeout) {
        var request = webClient.requestAbs(HttpMethod.valueOf(method), uri.toString());

        headers.forEach(request::putHeader);

        if (timeout != null) {
            request.timeout(timeout.toMillis());
        }

        CompletableFuture<RawResponse> future = new CompletableFuture<>();

        Handler<AsyncResult<HttpResponse<Buffer>>> handler = ar -> {
            if (ar.succeeded()) {
                HttpResponse<Buffer> response = ar.result();
                int statusCode = response.statusCode();
                String responseBody = response.bodyAsString();
                if (statusCode >= 400) {
                    future.completeExceptionally(new ApiException(statusCode, responseBody));
                } else {
                    future.complete(new RawResponse(statusCode,
                            toHeaderMap(response.headers()), responseBody));
                }
            } else {
                future.completeExceptionally(
                        new ArkException("API request failed: " + ar.cause().getMessage(), ar.cause()));
            }
        };

        if (body != null) {
            request.sendBuffer(Buffer.buffer(body), handler);
        } else {
            request.send(handler);
        }

        return future;
    }

    private Map<String, List<String>> toHeaderMap(io.vertx.core.MultiMap multiMap) {
        Map<String, List<String>> headers = new HashMap<>();
        multiMap.forEach(entry ->
                headers.computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
                        .add(entry.getValue()));
        return headers;
    }
}
