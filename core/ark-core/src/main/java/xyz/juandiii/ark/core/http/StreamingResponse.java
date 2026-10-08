package xyz.juandiii.ark.core.http;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A response whose body has not been read yet. Always use try-with-resources:
 * closing releases the connection even if the body was not fully read.
 *
 * <p>{@link #body()} can only be consumed once. Response interceptors are not
 * applied to streaming responses.
 *
 * <p>The request/read timeout only covers receiving the status line and headers.
 * Reading the body has no per-read timeout, so a server that stalls mid-body can
 * block {@code read()} until the connection drops. Callers that need a bound can
 * close the response from another thread.
 *
 * @author Juan Diego Lopez V.
 */
public interface StreamingResponse extends AutoCloseable {

    /**
     * @return the HTTP status code
     */
    int statusCode();

    /**
     * @return the response headers
     */
    Map<String, List<String>> headers();

    /**
     * Returns the first value of the given header, matching the name case-insensitively.
     *
     * @param name header name
     * @return the first value, or empty if absent
     */
    Optional<String> header(String name);

    /**
     * @return the body stream; never null (empty for bodiless responses). Can be consumed once.
     */
    InputStream body();

    /**
     * Release the connection. Safe to call more than once.
     */
    @Override
    void close();
}
