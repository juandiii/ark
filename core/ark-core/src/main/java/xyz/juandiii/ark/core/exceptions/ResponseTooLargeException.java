package xyz.juandiii.ark.core.exceptions;

import java.net.URI;

/**
 * Exception thrown when an HTTP response body exceeds the configured maximum size.
 *
 * @author Juan Diego Lopez V.
 */
public class ResponseTooLargeException extends ArkException {

    private final long limit;

    public ResponseTooLargeException(long limit) {
        super("Response body exceeded limit of " + limit + " bytes");
        this.limit = limit;
    }

    public ResponseTooLargeException(String method, URI uri, long limit) {
        super(method, uri, "Response body exceeded limit of " + limit + " bytes", null);
        this.limit = limit;
    }

    /**
     * @return the configured maximum response size in bytes
     */
    public long limit() {
        return limit;
    }
}
