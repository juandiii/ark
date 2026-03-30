package xyz.juandiii.ark.transport.jdk;

import xyz.juandiii.ark.core.exceptions.ApiException;
import xyz.juandiii.ark.core.exceptions.ArkException;
import xyz.juandiii.ark.core.exceptions.RequestInterruptedException;
import xyz.juandiii.ark.async.http.AsyncHttpTransport;
import xyz.juandiii.ark.core.http.HttpTransport;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.http.TransportLogger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

/**
 * HTTP transport bridge using Java's native HttpClient (sync + async).
 *
 * @author Juan Diego Lopez V.
 */
public final class ArkJdkHttpTransport implements HttpTransport, AsyncHttpTransport {

    private final HttpClient httpClient;
    private final Executor executor;

    public ArkJdkHttpTransport(HttpClient httpClient) {
        this(httpClient, httpClient.executor().orElse(Runnable::run));
    }

    public ArkJdkHttpTransport(HttpClient httpClient, Executor executor) {
        Objects.requireNonNull(httpClient, "HttpClient is required");
        Objects.requireNonNull(executor, "Executor must not be null");
        this.httpClient = httpClient;
        this.executor = executor;
    }

    private static final System.Logger LOGGER = System.getLogger(ArkJdkHttpTransport.class.getName());

    @Override
    public RawResponse send(String method, URI uri, Map<String, String> headers,
                            String body, Duration timeout) {
        return doSend(method, uri, headers,
                buildRequest(method, uri, headers, toPublisher(body), timeout),
                body);
    }

    @Override
    public RawResponse sendBinary(String method, URI uri, Map<String, String> headers,
                                   byte[] body, Duration timeout) {
        return doSend(method, uri, headers,
                buildRequest(method, uri, headers, toPublisher(body), timeout),
                body != null ? "[binary: " + body.length + " bytes]" : null);
    }

    @Override
    public CompletableFuture<RawResponse> sendAsync(String method, URI uri,
                                                    Map<String, String> headers,
                                                    String body, Duration timeout) {
        return doSendAsync(method, uri, headers,
                buildRequest(method, uri, headers, toPublisher(body), timeout),
                body);
    }

    @Override
    public CompletableFuture<RawResponse> sendBinaryAsync(String method, URI uri,
                                                           Map<String, String> headers,
                                                           byte[] body, Duration timeout) {
        return doSendAsync(method, uri, headers,
                buildRequest(method, uri, headers, toPublisher(body), timeout),
                body != null ? "[binary: " + body.length + " bytes]" : null);
    }

    private RawResponse doSend(String method, URI uri, Map<String, String> headers,
                                HttpRequest request, Object logBody) {
        LOGGER.log(System.Logger.Level.DEBUG, () ->
                TransportLogger.formatRequest(method, uri, headers, logBody != null ? logBody.toString() : null));
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LOGGER.log(System.Logger.Level.DEBUG, () ->
                    TransportLogger.formatResponse(response.statusCode(), response.headers().map(), response.body()));
            return toRawResponse(response);
        } catch (IOException e) {
            throw ArkException.fromIOException(method, uri, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RequestInterruptedException(method, uri, "Request interrupted", e);
        }
    }

    private CompletableFuture<RawResponse> doSendAsync(String method, URI uri, Map<String, String> headers,
                                                        HttpRequest request, Object logBody) {
        LOGGER.log(System.Logger.Level.DEBUG, () ->
                TransportLogger.formatRequest(method, uri, headers, logBody != null ? logBody.toString() : null));
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(response -> {
                    if (RawResponse.isErrorStatus(response.statusCode())) {
                        throw new CompletionException(
                                ApiException.of(response.statusCode(), response.body()));
                    }
                    return new RawResponse(response.statusCode(), response.headers().map(), response.body());
                }, executor);
    }

    private HttpRequest buildRequest(String method, URI uri, Map<String, String> headers,
                                      HttpRequest.BodyPublisher bodyPublisher, Duration timeout) {
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(uri);
        if (timeout != null) builder.timeout(timeout);
        headers.forEach(builder::header);
        return builder.method(method, bodyPublisher).build();
    }

    private static HttpRequest.BodyPublisher toPublisher(String body) {
        return body != null ? HttpRequest.BodyPublishers.ofString(body) : HttpRequest.BodyPublishers.noBody();
    }

    private static HttpRequest.BodyPublisher toPublisher(byte[] body) {
        return body != null ? HttpRequest.BodyPublishers.ofByteArray(body) : HttpRequest.BodyPublishers.noBody();
    }

    private RawResponse toRawResponse(HttpResponse<String> response) {
        if (RawResponse.isErrorStatus(response.statusCode())) {
            throw ApiException.of(response.statusCode(), response.body());
        }
        return new RawResponse(response.statusCode(), response.headers().map(), response.body());
    }
}
