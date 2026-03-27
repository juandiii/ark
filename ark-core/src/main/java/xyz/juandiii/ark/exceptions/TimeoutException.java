package xyz.juandiii.ark.exceptions;

import java.net.URI;

/**
 * Exception thrown when an HTTP request times out (connect or read timeout).
 *
 * @author Juan Diego Lopez V.
 */
public class TimeoutException extends ArkException {

    public TimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    public TimeoutException(String method, URI uri, String message, Throwable cause) {
        super(method, uri, message, cause);
    }
}