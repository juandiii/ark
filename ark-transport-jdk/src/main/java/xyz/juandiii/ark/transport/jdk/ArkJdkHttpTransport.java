package xyz.juandiii.ark.transport.jdk;

import xyz.juandiii.ark.exceptions.ApiException;
import xyz.juandiii.ark.exceptions.ArkException;
import xyz.juandiii.ark.async.http.AsyncHttpTransport;
import xyz.juandiii.ark.http.HttpTransport;
import xyz.juandiii.ark.http.RawResponse;
import xyz.juandiii.ark.http.TransportLogger;

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
        LOGGER.log(System.Logger.Level.DEBUG, () -> TransportLogger.formatRequest(method, uri, headers, body));
        HttpRequest request = buildRequest(method, uri, headers, body, timeout);

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LOGGER.log(System.Logger.Level.DEBUG, () -> TransportLogger.formatResponse(response.statusCode(), response.headers().map(), response.body()));
            return toRawResponse(response);
        } catch (IOException e) {
            throw new ArkException("API request failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ArkException("API request interrupted", e);
        }
    }

    @Override
    public CompletableFuture<RawResponse> sendAsync(String method, URI uri,
                                                    Map<String, String> headers,
                                                    String body, Duration timeout) {
        LOGGER.log(System.Logger.Level.DEBUG, () -> TransportLogger.formatRequest(method, uri, headers, body));
        HttpRequest request = buildRequest(method, uri, headers, body, timeout);

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(response -> {
                    if (RawResponse.isErrorStatus(response.statusCode())) {
                        throw new CompletionException(
                                new ApiException(response.statusCode(), response.body()));
                    }
                    return new RawResponse(response.statusCode(), response.headers().map(), response.body());
                }, executor);
    }

    private HttpRequest buildRequest(String method, URI uri, Map<String, String> headers,
                                     String body, Duration timeout) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri);

        if (timeout != null) {
            builder.timeout(timeout);
        }

        headers.forEach(builder::header);

        HttpRequest.BodyPublisher bodyPublisher = body != null
                ? HttpRequest.BodyPublishers.ofString(body)
                : HttpRequest.BodyPublishers.noBody();

        return builder.method(method, bodyPublisher).build();
    }

    private RawResponse toRawResponse(HttpResponse<String> response) {
        if (RawResponse.isErrorStatus(response.statusCode())) {
            throw new ApiException(response.statusCode(), response.body());
        }
        return new RawResponse(response.statusCode(), response.headers().map(), response.body());
    }
}
