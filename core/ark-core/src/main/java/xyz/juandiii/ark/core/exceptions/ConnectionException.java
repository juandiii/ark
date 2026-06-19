package xyz.juandiii.ark.core.exceptions;

import java.net.URI;

/**
 * Exception thrown when a connection to the remote server fails
 * (connection refused, DNS resolution failure, connection reset).
 *
 * @author Juan Diego Lopez V.
 */
public class ConnectionException extends ArkException {

    public ConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConnectionException(String method, URI uri, String message, Throwable cause) {
        super(method, uri, message, cause);
    }
}