package xyz.juandiii.ark.core.exceptions;

/**
 * Exception for HTTP 404 Not Found.
 *
 * @author Juan Diego Lopez V.
 */
public class NotFoundException extends ClientException {

    public NotFoundException(String responseBody) {
        super(404, responseBody);
    }
}