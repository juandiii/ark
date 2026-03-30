package xyz.juandiii.ark.transport.vertx;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import xyz.juandiii.ark.core.exceptions.ApiException;
import xyz.juandiii.ark.core.exceptions.ArkException;
import xyz.juandiii.ark.core.http.HeaderUtils;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.http.TransportLogger;
import xyz.juandiii.ark.vertx.http.VertxHttpTransport;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * HTTP transport bridge using Vert.x WebClient with Future.
 *
 * @author Juan Diego Lopez V.
 */
public final class ArkVertxFutureTransport implements VertxHttpTransport {

    private final WebClient webClient;

    public ArkVertxFutureTransport(WebClient webClient) {
        Objects.requireNonNull(webClient, "WebClient is required");
        this.webClient = webClient;
    }

    private static final System.Logger LOGGER = System.getLogger(ArkVertxFutureTransport.class.getName());

    @Override
    public Future<RawResponse> send(String method, URI uri, Map<String, String> headers,
                                    String body, Duration timeout) {
        LOGGER.log(System.Logger.Level.DEBUG, () -> TransportLogger.formatRequest(method, uri, headers, body));
        return execute(method, uri, headers, body != null ? Buffer.buffer(body) : null, timeout);
    }

    @Override
    public Future<RawResponse> sendBinary(String method, URI uri, Map<String, String> headers,
                                           byte[] body, Duration timeout) {
        LOGGER.log(System.Logger.Level.DEBUG, () ->
                TransportLogger.formatRequest(method, uri, headers, body != null ? "[binary: " + body.length + " bytes]" : null));
        return execute(method, uri, headers, body != null ? Buffer.buffer(body) : null, timeout);
    }

    private Future<RawResponse> execute(String method, URI uri, Map<String, String> headers,
                                         Buffer buffer, Duration timeout) {
        var request = webClient.requestAbs(HttpMethod.valueOf(method), uri.toString());
        headers.forEach(request::putHeader);

        if (timeout != null) {
            request.timeout(timeout.toMillis());
        }

        Future<HttpResponse<Buffer>> response = buffer != null
                ? request.sendBuffer(buffer)
                : request.send();

        return response
                .map(this::handleResponse)
                .otherwise(e -> {
                    throw (e instanceof ArkException || e instanceof ApiException)
                            ? (RuntimeException) e
                            : ArkException.fromThrowable(method, uri, e);
                });
    }

    private RawResponse handleResponse(HttpResponse<Buffer> r) {
        int statusCode = r.statusCode();
        String responseBody = r.bodyAsString();
        var responseHeaders = HeaderUtils.toHeaderMap(r.headers());
        LOGGER.log(System.Logger.Level.DEBUG, () -> TransportLogger.formatResponse(statusCode, responseHeaders, responseBody));
        if (RawResponse.isErrorStatus(statusCode)) {
            throw ApiException.of(statusCode, responseBody);
        }
        return new RawResponse(statusCode, responseHeaders, responseBody);
    }
}
