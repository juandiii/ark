package xyz.juandiii.ark.transport.jdk;

import xyz.juandiii.ark.core.exceptions.ArkException;
import xyz.juandiii.ark.core.exceptions.RequestInterruptedException;
import xyz.juandiii.ark.core.http.HttpTransport;
import xyz.juandiii.ark.core.http.RawResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Synchronous HTTP transport bridge using Java's native {@link HttpClient}.
 *
 * <p>Constructed independently from {@link ArkJdkAsyncTransport}; both can wrap
 * the same underlying {@link HttpClient} instance. The previous dual-mode
 * {@code ArkJdkHttpTransport} was removed when the transport layer unified
 * under {@code Transport<R>} (see spike 016 / plan 017 Nivel 2).
 *
 * @author Juan Diego Lopez V.
 */
public final class ArkJdkSyncTransport implements HttpTransport {

    private final HttpClient httpClient;

    public ArkJdkSyncTransport(HttpClient httpClient) {
        Objects.requireNonNull(httpClient, "HttpClient is required");
        this.httpClient = httpClient;
    }

    @Override
    public RawResponse send(String method, URI uri, Map<String, String> headers,
                            String body, Duration timeout) {
        return doSend(method, uri,
                ArkJdkRequestBuilder.build(method, uri, headers, ArkJdkRequestBuilder.toPublisher(body), timeout));
    }

    @Override
    public RawResponse sendBinary(String method, URI uri, Map<String, String> headers,
                                   byte[] body, Duration timeout) {
        return doSend(method, uri,
                ArkJdkRequestBuilder.build(method, uri, headers, ArkJdkRequestBuilder.toPublisher(body), timeout));
    }

    private RawResponse doSend(String method, URI uri, HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return new RawResponse(response.statusCode(), response.headers().map(), response.body());
        } catch (IOException e) {
            throw ArkException.fromIOException(method, uri, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RequestInterruptedException(method, uri, "Request interrupted", e);
        }
    }
}
