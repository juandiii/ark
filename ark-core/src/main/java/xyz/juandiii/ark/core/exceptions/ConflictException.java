package xyz.juandiii.ark.core.exceptions;

/**
 * Exception for HTTP 409 Conflict.
 *
 * @author Juan Diego Lopez V.
 */
public class ConflictException extends ClientException {

    public ConflictException(String responseBody) {
        super(409, responseBody);
    }
}