package xyz.juandiii.ark.core.exceptions;

import java.net.URI;

/**
 * Exception thrown when an HTTP request is interrupted.
 *
 * @author Juan Diego Lopez V.
 */
public class RequestInterruptedException extends ArkException {

    public RequestInterruptedException(String message, Throwable cause) {
        super(message, cause);
    }

    public RequestInterruptedException(String method, URI uri, String message, Throwable cause) {
        super(method, uri, message, cause);
    }
}