package xyz.juandiii.ark.core.http;

import java.net.URI;
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
public interface HttpTransport extends Transport<RawResponse> {

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
    @Override
    RawResponse send(String method, URI uri, Map<String, String> headers, String body, Duration timeout);

    /**
     * Send a binary request and return the raw response synchronously.
     * Transport implementations MUST override this method when supporting
     * binary bodies; the default throws to prevent silent data corruption.
     *
     * @throws UnsupportedOperationException if the transport does not implement binary upload
     * @throws xyz.juandiii.ark.core.exceptions.ArkException on transport/IO errors
     */
    @Override
    default RawResponse sendBinary(String method, URI uri, Map<String, String> headers,
                                    byte[] body, Duration timeout) {
        return Transport.super.sendBinary(method, uri, headers, body, timeout);
    }
}
