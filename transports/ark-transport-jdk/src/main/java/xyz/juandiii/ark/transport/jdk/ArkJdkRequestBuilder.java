package xyz.juandiii.ark.transport.jdk;

import java.net.URI;
import java.net.http.HttpRequest;
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

    static HttpRequest.BodyPublisher toPublisher(String body) {
        return body != null ? HttpRequest.BodyPublishers.ofString(body) : HttpRequest.BodyPublishers.noBody();
    }

    static HttpRequest.BodyPublisher toPublisher(byte[] body) {
        return body != null ? HttpRequest.BodyPublishers.ofByteArray(body) : HttpRequest.BodyPublishers.noBody();
    }
}
