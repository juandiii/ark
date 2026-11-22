package xyz.juandiii.ark.transport.vertx.mutiny;

import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.HttpResponse;
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
 * <p>
 * Requests go through the WebClient's core delegate, whose API is the same in Vert.x 4 and 5,
 * so this transport works with both Mutiny binding generations (3.x for Vert.x 4, 4.x for Vert.x 5).
 *
 * @author Juan Diego Lopez V.
 */
public final class ArkVertxMutinyTransport implements MutinyHttpTransport {

    private final io.vertx.ext.web.client.WebClient webClient;

    public ArkVertxMutinyTransport(WebClient webClient) {
        Objects.requireNonNull(webClient, "WebClient is required");
        this.webClient = webClient.getDelegate();
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

        // Supplier form keeps the Uni lazy: each subscription (e.g. a retry) sends a new request.
        return Uni.createFrom().completionStage(() -> (buffer != null
                        ? request.sendBuffer(buffer)
                        : request.send()).toCompletionStage())
                .onItem().transform(this::toRawResponse)
                .onFailure().transform(e -> ArkException.fromThrowable(method, uri, e));
    }

    private RawResponse toRawResponse(HttpResponse<Buffer> r) {
        Buffer b = r.bodyAsBuffer();
        return RawResponse.ofBytes(r.statusCode(), HeaderUtils.toHeaderMap(r.headers()), b != null ? b.getBytes() : null);
    }
}
