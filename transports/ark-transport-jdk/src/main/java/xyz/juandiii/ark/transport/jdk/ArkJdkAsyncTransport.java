package xyz.juandiii.ark.transport.jdk;

import xyz.juandiii.ark.core.exceptions.ResponseTooLargeException;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.http.Transport;

import java.net.URI;
import java.net.http.HttpClient;
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
    private final long maxResponseBytes;

    public ArkJdkAsyncTransport(HttpClient httpClient) {
        this(httpClient, httpClient.executor().orElse(Runnable::run));
    }

    public ArkJdkAsyncTransport(HttpClient httpClient, Executor executor) {
        this(httpClient, executor, 0);
    }

    /**
     * @param httpClient       the pre-configured HTTP client
     * @param executor         executor used to complete responses
     * @param maxResponseBytes maximum response body size in bytes; a value {@code <= 0} means no limit
     */
    public ArkJdkAsyncTransport(HttpClient httpClient, Executor executor, long maxResponseBytes) {
        Objects.requireNonNull(httpClient, "HttpClient is required");
        Objects.requireNonNull(executor, "Executor must not be null");
        this.httpClient = httpClient;
        this.executor = executor;
        this.maxResponseBytes = maxResponseBytes;
    }

    @Override
    public CompletableFuture<RawResponse> send(String method, URI uri, Map<String, String> headers,
                                                String body, Duration timeout) {
        return doSendAsync(method, uri, ArkJdkRequestBuilder.build(method, uri, headers,
                ArkJdkRequestBuilder.toPublisher(body), timeout));
    }

    @Override
    public CompletableFuture<RawResponse> sendBinary(String method, URI uri, Map<String, String> headers,
                                                      byte[] body, Duration timeout) {
        return doSendAsync(method, uri, ArkJdkRequestBuilder.build(method, uri, headers,
                ArkJdkRequestBuilder.toPublisher(body), timeout));
    }

    private CompletableFuture<RawResponse> doSendAsync(String method, URI uri, java.net.http.HttpRequest request) {
        return httpClient.sendAsync(request, ArkJdkRequestBuilder.bodyHandler(maxResponseBytes))
                .thenApplyAsync(response ->
                        RawResponse.ofBytes(response.statusCode(), response.headers().map(), response.body()),
                        executor)
                .handle((response, error) -> {
                    if (error == null) return CompletableFuture.completedFuture(response);
                    ResponseTooLargeException tooLarge = ArkJdkRequestBuilder.findTooLarge(error);
                    return CompletableFuture.<RawResponse>failedFuture(tooLarge != null
                            ? new ResponseTooLargeException(method, uri, tooLarge.limit())
                            : error);
                })
                .thenCompose(f -> f);
    }
}
