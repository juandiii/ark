package xyz.juandiii.ark.transport.vertx.mutiny;

import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpMethod;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import xyz.juandiii.ark.core.exceptions.ArkException;
import xyz.juandiii.ark.core.http.HeaderUtils;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.mutiny.http.MutinyHttpTransport;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * HTTP transport bridge using Vert.x Mutiny WebClient.
 *
 * @author Juan Diego Lopez V.
 */
public final class ArkVertxMutinyTransport implements MutinyHttpTransport {

    private final WebClient webClient;

    public ArkVertxMutinyTransport(WebClient webClient) {
        Objects.requireNonNull(webClient, "WebClient is required");
        this.webClient = webClient;
    }

    @Override
    public Uni<RawResponse> send(String method, URI uri, Map<String, String> headers,
                                 String body, Duration timeout) {
        return execute(method, uri, headers, body != null ? Buffer.buffer(body) : null, timeout);
    }

    @Override
    public Uni<RawResponse> sendBinary(String method, URI uri, Map<String, String> headers,
                                        byte[] body, Duration timeout) {
        return execute(method, uri, headers, body != null ? Buffer.buffer(body) : null, timeout);
    }

    private Uni<RawResponse> execute(String method, URI uri, Map<String, String> headers,
                                      Buffer buffer, Duration timeout) {
        var request = webClient.requestAbs(HttpMethod.valueOf(method), uri.toString());
        headers.forEach(request::putHeader);

        if (timeout != null) {
            request.timeout(timeout.toMillis());
        }

        Uni<HttpResponse<Buffer>> response = buffer != null
                ? request.sendBuffer(buffer)
                : request.send();

        return response.onItem()
                .transform(r -> new RawResponse(
                        r.statusCode(),
                        HeaderUtils.toHeaderMap(r.headers().getDelegate()),
                        r.bodyAsString()))
                .onFailure().transform(e -> ArkException.fromThrowable(method, uri, e));
    }
}
