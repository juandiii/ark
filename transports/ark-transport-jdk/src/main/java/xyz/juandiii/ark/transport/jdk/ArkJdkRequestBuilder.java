package xyz.juandiii.ark.transport.jdk;

import java.net.URI;
import xyz.juandiii.ark.core.exceptions.ResponseTooLargeException;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

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
     * @param maxBytes maximum body size in bytes; a value {@code <= 0} means no limit
     * @return the body handler
     */
    static HttpResponse.BodyHandler<byte[]> bodyHandler(long maxBytes) {
        if (maxBytes <= 0) return HttpResponse.BodyHandlers.ofByteArray();
        return info -> new LimitedByteArraySubscriber(maxBytes,
                info.headers().firstValueAsLong("Content-Length").orElse(-1));
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
