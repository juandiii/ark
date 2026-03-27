package xyz.juandiii.ark.transport.vertx.mutiny;

import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpMethod;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import xyz.juandiii.ark.exceptions.ApiException;
import xyz.juandiii.ark.exceptions.ArkException;
import xyz.juandiii.ark.http.HeaderUtils;
import xyz.juandiii.ark.http.RawResponse;
import xyz.juandiii.ark.http.TransportLogger;
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

    private static final System.Logger LOGGER = System.getLogger(ArkVertxMutinyTransport.class.getName());

    @Override
    public Uni<RawResponse> send(String method, URI uri, Map<String, String> headers,
                                 String body, Duration timeout) {
        LOGGER.log(System.Logger.Level.DEBUG, () -> TransportLogger.formatRequest(method, uri, headers, body));
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
                    // Unwrap Mutiny MultiMap to Vert.x core MultiMap for header conversion
                    var responseHeaders = HeaderUtils.toHeaderMap(r.headers().getDelegate());
                    LOGGER.log(System.Logger.Level.DEBUG, () -> TransportLogger.formatResponse(statusCode, responseHeaders, responseBody));
                    if (RawResponse.isErrorStatus(statusCode)) {
                        throw ApiException.of(statusCode, responseBody);
                    }
                    return new RawResponse(statusCode, responseHeaders, responseBody);
                })
                .onFailure().transform(e -> (e instanceof ArkException || e instanceof ApiException) ? e : ArkException.fromThrowable(method, uri, e));
    }
}
