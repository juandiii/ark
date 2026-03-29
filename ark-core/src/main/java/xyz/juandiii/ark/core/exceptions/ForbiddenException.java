package xyz.juandiii.ark.core.exceptions;

/**
 * Exception for HTTP 403 Forbidden.
 *
 * @author Juan Diego Lopez V.
 */
public class ForbiddenException extends ClientException {

    public ForbiddenException(String responseBody) {
        super(403, responseBody);
    }
}