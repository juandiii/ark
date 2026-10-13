package xyz.juandiii.ark.core.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default implementation of {@link StreamingResponse}.
 *
 * @author Juan Diego Lopez V.
 */
public final class DefaultStreamingResponse implements StreamingResponse {

    private final int statusCode;
    private final Map<String, List<String>> headers;
    private final InputStream body;
    private final Runnable onClose;
    private final AtomicBoolean closed = new AtomicBoolean();

    /**
     * @param statusCode HTTP status code
     * @param headers    response headers
     * @param body       body stream; {@code null} becomes an empty stream
     * @param onClose    optional hook run once after the body is closed; may be {@code null}
     */
    public DefaultStreamingResponse(int statusCode, Map<String, List<String>> headers,
                                    InputStream body, Runnable onClose) {
        this.statusCode = statusCode;
        this.headers = headers;
        this.body = body != null ? body : InputStream.nullInputStream();
        this.onClose = onClose;
    }

    @Override
    public int statusCode() {
        return statusCode;
    }

    @Override
    public Map<String, List<String>> headers() {
        return headers;
    }

    @Override
    public Optional<String> header(String name) {
        return RawResponse.firstHeader(headers, name);
    }

    @Override
    public InputStream body() {
        return body;
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        try {
            body.close();
        } catch (IOException ignored) {
            // best effort
        }
        if (onClose != null) onClose.run();
    }
}
