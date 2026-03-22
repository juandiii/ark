package xyz.juandiii.ark.transport.vertx.mutiny;

import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpMethod;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import xyz.juandiii.ark.exceptions.ApiException;
import xyz.juandiii.ark.http.RawResponse;
import xyz.juandiii.ark.mutiny.http.MutinyHttpTransport;

import java.net.URI;
import java.time.Duration;
import java.util.*;

public final class VertxMutinyTransport implements MutinyHttpTransport {

    private final WebClient webClient;

    public VertxMutinyTransport(WebClient webClient) {
        Objects.requireNonNull(webClient, "WebClient is required");
        this.webClient = webClient;
    }

    @Override
    public Uni<RawResponse> send(String method, URI uri, Map<String, String> headers,
                                 String body, Duration timeout) {
        var request = webClient.requestAbs(HttpMethod.valueOf(method), uri.toString());

        headers.forEach(request::putHeader);

        if (timeout != null) {
            request.timeout(timeout.toMillis());
        }

        Uni<HttpResponse<Buffer>> response = body != null
                ? request.sendBuffer(Buffer.buffer(body))
                : request.send();

        return response.onItem()
                .transform(r -> {
                    int statusCode = r.statusCode();
                    String responseBody = r.bodyAsString();
                    if (statusCode >= 400) {
                        throw new ApiException(statusCode, responseBody);
                    }
                    return new RawResponse(statusCode, toHeaderMap(r.headers()), responseBody);
                });
    }

    private Map<String, List<String>> toHeaderMap(io.vertx.mutiny.core.MultiMap multiMap) {
        Map<String, List<String>> headers = new HashMap<>();
        multiMap.getDelegate().forEach(entry ->
                headers.computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
                        .add(entry.getValue()));
        return headers;
    }
}
