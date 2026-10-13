package xyz.juandiii.ark.transport.jdk;

import xyz.juandiii.ark.core.exceptions.ArkException;
import xyz.juandiii.ark.core.exceptions.RequestInterruptedException;
import xyz.juandiii.ark.core.exceptions.ResponseTooLargeException;
import xyz.juandiii.ark.core.http.HttpTransport;
import xyz.juandiii.ark.core.http.DefaultStreamingResponse;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.http.StreamingResponse;
import xyz.juandiii.ark.core.http.StreamingTransport;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

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
public final class ArkJdkSyncTransport implements HttpTransport, StreamingTransport {

    private final HttpClient httpClient;
    private final long maxResponseBytes;

    public ArkJdkSyncTransport(HttpClient httpClient) {
        this(httpClient, 0);
    }

    /**
     * @param httpClient       the pre-configured HTTP client
     * @param maxResponseBytes maximum response body size in bytes; a value {@code <= 0} means no limit
     */
    public ArkJdkSyncTransport(HttpClient httpClient, long maxResponseBytes) {
        Objects.requireNonNull(httpClient, "HttpClient is required");
        this.httpClient = httpClient;
        this.maxResponseBytes = maxResponseBytes;
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

    /**
     * Sends the request and returns the response with an unread body stream.
     * Closing the returned response releases the connection, even if the body
     * was not fully read.
     *
     * @param method  HTTP method
     * @param uri     target URI
     * @param headers request headers
     * @param body    text request body, or {@code null}
     * @param timeout request timeout, or {@code null}
     * @return the streaming response; the caller must close it
     * @throws ResponseTooLargeException if Content-Length is known and exceeds the limit,
     *                                   or later from {@code read} if the limit trips while reading
     * @throws ArkException              for transport/IO failures
     */
    @Override
    public StreamingResponse sendStreaming(String method, URI uri, Map<String, String> headers,
                                           String body, Duration timeout) {
        HttpRequest request = ArkJdkRequestBuilder.build(method, uri, headers,
                ArkJdkRequestBuilder.toPublisher(body), timeout);
        HttpResponse<InputStream> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException e) {
            throw ArkException.fromIOException(method, uri, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RequestInterruptedException(method, uri, "Request interrupted", e);
        }
        InputStream in = response.body();
        int status = response.statusCode();
        if (maxResponseBytes > 0 && ArkJdkRequestBuilder.mayHaveBody(method, status)) {
            long len = response.headers().firstValueAsLong("Content-Length").orElse(-1);
            if (len > maxResponseBytes) {
                try {
                    in.close();
                } catch (IOException ignored) {
                    // best effort
                }
                throw new ResponseTooLargeException(method, uri, maxResponseBytes);
            }
            in = new LimitedInputStream(in, maxResponseBytes, method, uri);
        }
        return new DefaultStreamingResponse(status, response.headers().map(), in, null);
    }

    private RawResponse doSend(String method, URI uri, HttpRequest request) {
        AtomicBoolean exceeded = new AtomicBoolean();
        try {
            HttpResponse<byte[]> response = httpClient.send(request,
                    ArkJdkRequestBuilder.bodyHandler(maxResponseBytes, method, exceeded));
            return RawResponse.ofBytes(response.statusCode(), response.headers().map(), response.body());
        } catch (IOException e) {
            if (exceeded.get() || ArkJdkRequestBuilder.findTooLarge(e) != null) {
                throw new ResponseTooLargeException(method, uri, maxResponseBytes);
            }
            throw ArkException.fromIOException(method, uri, e);
        } catch (ResponseTooLargeException e) {
            throw new ResponseTooLargeException(method, uri, maxResponseBytes);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RequestInterruptedException(method, uri, "Request interrupted", e);
        }
    }
}
