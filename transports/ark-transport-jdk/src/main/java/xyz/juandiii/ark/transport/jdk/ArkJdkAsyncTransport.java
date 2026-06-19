package xyz.juandiii.ark.transport.jdk;

import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.http.Transport;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Asynchronous HTTP transport bridge using Java's native {@link HttpClient}.
 * Implements {@code Transport<CompletableFuture<RawResponse>>} so it composes
 * with the generic decorator chain ({@code .with(Retry.of(...))}, etc.).
 *
 * <p>Constructed independently from {@link ArkJdkSyncTransport}; both can wrap
 * the same underlying {@link HttpClient} instance.
 *
 * @author Juan Diego Lopez V.
 */
public final class ArkJdkAsyncTransport implements Transport<CompletableFuture<RawResponse>> {

    private final HttpClient httpClient;
    private final Executor executor;

    public ArkJdkAsyncTransport(HttpClient httpClient) {
        this(httpClient, httpClient.executor().orElse(Runnable::run));
    }

    public ArkJdkAsyncTransport(HttpClient httpClient, Executor executor) {
        Objects.requireNonNull(httpClient, "HttpClient is required");
        Objects.requireNonNull(executor, "Executor must not be null");
        this.httpClient = httpClient;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<RawResponse> send(String method, URI uri, Map<String, String> headers,
                                                String body, Duration timeout) {
        return doSendAsync(ArkJdkRequestBuilder.build(method, uri, headers,
                ArkJdkRequestBuilder.toPublisher(body), timeout));
    }

    @Override
    public CompletableFuture<RawResponse> sendBinary(String method, URI uri, Map<String, String> headers,
                                                      byte[] body, Duration timeout) {
        return doSendAsync(ArkJdkRequestBuilder.build(method, uri, headers,
                ArkJdkRequestBuilder.toPublisher(body), timeout));
    }

    private CompletableFuture<RawResponse> doSendAsync(java.net.http.HttpRequest request) {
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(response ->
                        new RawResponse(response.statusCode(), response.headers().map(), response.body()),
                        executor);
    }
}
