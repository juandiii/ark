package xyz.juandiii.ark.transport.jdk;

import xyz.juandiii.ark.core.exceptions.ResponseTooLargeException;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Package-private helper that builds {@link HttpRequest} instances shared by
 * {@link ArkJdkSyncTransport} and {@link ArkJdkAsyncTransport}.
 */
final class ArkJdkRequestBuilder {

    private ArkJdkRequestBuilder() {}

    static HttpRequest build(String method, URI uri, Map<String, String> headers,
                              HttpRequest.BodyPublisher bodyPublisher, Duration timeout) {
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(uri);
        if (timeout != null) builder.timeout(timeout);
        headers.forEach(builder::header);
        return builder.method(method, bodyPublisher).build();
    }

    /**
     * Creates a body handler that reads the body as {@code byte[]}, aborting with a
     * {@link ResponseTooLargeException} when it exceeds {@code maxBytes}.
     *
     * <p>The declared {@code Content-Length} is used for early rejection only when the
     * response can carry a body (not HEAD, 1xx, 204 or 304); the streamed byte count is
     * always enforced.
     *
     * @param maxBytes maximum body size in bytes; a value {@code <= 0} means no limit
     * @param method   the HTTP method of the request
     * @return the body handler
     */
    static HttpResponse.BodyHandler<byte[]> bodyHandler(long maxBytes, String method) {
        return bodyHandler(maxBytes, method, new AtomicBoolean());
    }

    /**
     * Same as {@link #bodyHandler(long, String)} but records in {@code exceeded} when the limit trips,
     * so callers do not depend on how the JDK wraps the resulting failure.
     *
     * @param maxBytes maximum body size, {@code <= 0} for unlimited
     * @param method   the HTTP method
     * @param exceeded per-request flag set when the limit is exceeded
     * @return the body handler
     */
    static HttpResponse.BodyHandler<byte[]> bodyHandler(long maxBytes, String method, AtomicBoolean exceeded) {
        if (maxBytes <= 0) return HttpResponse.BodyHandlers.ofByteArray();
        boolean head = "HEAD".equalsIgnoreCase(method);
        return info -> {
            int status = info.statusCode();
            boolean bodiless = head || status < 200 || status == 204 || status == 304;
            long contentLength = bodiless ? -1
                    : info.headers().firstValueAsLong("Content-Length").orElse(-1);
            return new LimitedByteArraySubscriber(maxBytes, contentLength, exceeded);
        };
    }

    /**
     * Walks the cause chain looking for a {@link ResponseTooLargeException}.
     *
     * @param error the failure to inspect
     * @return the exception found, or {@code null}
     */
    static ResponseTooLargeException findTooLarge(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof ResponseTooLargeException tooLarge) return tooLarge;
        }
        return null;
    }

    static HttpRequest.BodyPublisher toPublisher(String body) {
        return body != null ? HttpRequest.BodyPublishers.ofString(body) : HttpRequest.BodyPublishers.noBody();
    }

    static HttpRequest.BodyPublisher toPublisher(byte[] body) {
        return body != null ? HttpRequest.BodyPublishers.ofByteArray(body) : HttpRequest.BodyPublishers.noBody();
    }
}
