package xyz.juandiii.ark.transport.vertx;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import xyz.juandiii.ark.exceptions.ApiException;
import xyz.juandiii.ark.http.RawResponse;
import xyz.juandiii.ark.vertx.http.VertxHttpTransport;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ArkVertxFutureTransport implements VertxHttpTransport {

    private final WebClient webClient;

    public ArkVertxFutureTransport(WebClient webClient) {
        Objects.requireNonNull(webClient, "WebClient is required");
        this.webClient = webClient;
    }

    @Override
    public Future<RawResponse> send(String method, URI uri, Map<String, String> headers,
                                    String body, Duration timeout) {
        var request = webClient.requestAbs(HttpMethod.valueOf(method), uri.toString());

        headers.forEach(request::putHeader);

        if (timeout != null) {
            request.timeout(timeout.toMillis());
        }

        Future<HttpResponse<Buffer>> response = body != null
                ? request.sendBuffer(Buffer.buffer(body))
                : request.send();

        return response.map(this::handleResponse);
    }

    private RawResponse handleResponse(HttpResponse<Buffer> r) {
        int statusCode = r.statusCode();
        String responseBody = r.bodyAsString();
        if (RawResponse.isErrorStatus(statusCode)) {
            throw new ApiException(statusCode, responseBody);
        }
        return new RawResponse(statusCode, toHeaderMap(r.headers()), responseBody);
    }

    private Map<String, List<String>> toHeaderMap(io.vertx.core.MultiMap multiMap) {
        Map<String, List<String>> headers = new HashMap<>();
        multiMap.forEach(entry ->
                headers.computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
                        .add(entry.getValue()));
        return headers;
    }
}
