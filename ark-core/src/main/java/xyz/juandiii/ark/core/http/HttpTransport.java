package xyz.juandiii.ark.core.http;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * Synchronous HTTP transport contract. Implementations bridge Ark to a specific
 * underlying HTTP client (JDK {@code HttpClient}, Apache HttpClient 5, Reactor
 * Netty, etc.). Implementations MUST be thread-safe; Ark calls {@link #send}
 * concurrently across threads.
 *
 * <p>Transport implementations do NOT configure the HTTP client (timeouts, SSL,
 * pools) — that is the responsibility of whoever constructs the underlying
 * client and passes it to the transport.
 *
 * @author Juan Diego Lopez V.
 */
@FunctionalInterface
public interface HttpTransport {

    /**
     * Send a text/JSON request and return the raw response synchronously.
     *
     * @param method   HTTP method (e.g. {@code "GET"}, {@code "POST"}); never {@code null}
     * @param uri      fully-qualified target URI
     * @param headers  request headers (caller-owned; transport MUST NOT mutate)
     * @param body     request body as text; {@code null} for bodiless methods
     * @param timeout  per-request timeout; {@code null} uses the underlying client's default
     * @return raw response with status, headers and body
     * @throws xyz.juandiii.ark.core.exceptions.ArkException on transport/IO errors (timeout, connection refused, interrupted)
     */
    RawResponse send(String method, URI uri, Map<String, String> headers, String body, Duration timeout);

    /**
     * Send a binary request and return the raw response synchronously.
     *
     * <p>The default implementation falls back to {@link #send} after a UTF-8
     * decode of {@code body} — that is LOSSY for non-UTF8 bytes. Transports
     * that handle binary uploads (multipart, gzipped, image, protobuf) MUST
     * override this to preserve byte-for-byte fidelity.
     *
     * @param method   HTTP method
     * @param uri      fully-qualified target URI
     * @param headers  request headers (caller-owned; transport MUST NOT mutate)
     * @param body     binary request body; {@code null} for bodiless methods
     * @param timeout  per-request timeout; {@code null} uses the underlying client's default
     * @return raw response with status, headers and body
     * @throws xyz.juandiii.ark.core.exceptions.ArkException on transport/IO errors
     */
    default RawResponse sendBinary(String method, URI uri, Map<String, String> headers,
                                    byte[] body, Duration timeout) {
        return send(method, uri, headers, body != null ? new String(body, StandardCharsets.UTF_8) : null, timeout);
    }
}
