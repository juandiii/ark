package xyz.juandiii.ark.core.http;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

/**
 * Capability interface for transports that can return the response body as a
 * stream instead of buffering it. Sync-only; deliberately separate from
 * {@link Transport}.
 *
 * <p>The caller MUST close the returned {@link StreamingResponse}.
 *
 * @author Juan Diego Lopez V.
 */
public interface StreamingTransport {

    /**
     * Send a request and return the response with an unread body stream.
     *
     * @param method  HTTP method
     * @param uri     target URI
     * @param headers request headers
     * @param body    text request body, or {@code null}
     * @param timeout request timeout, or {@code null}
     * @return the streaming response; the caller must close it
     * @throws xyz.juandiii.ark.core.exceptions.ArkException for transport/IO failures
     */
    StreamingResponse sendStreaming(String method, URI uri, Map<String, String> headers,
                                    String body, Duration timeout);
}
