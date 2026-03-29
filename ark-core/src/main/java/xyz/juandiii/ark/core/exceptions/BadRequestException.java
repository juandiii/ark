package xyz.juandiii.ark.core.exceptions;

/**
 * Exception for HTTP 400 Bad Request.
 *
 * @author Juan Diego Lopez V.
 */
public class BadRequestException extends ClientException {

    public BadRequestException(String responseBody) {
        super(400, responseBody);
    }
}