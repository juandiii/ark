package xyz.juandiii.ark.core.exceptions;

/**
 * Exception for HTTP 500 Internal Server Error.
 *
 * @author Juan Diego Lopez V.
 */
public class InternalServerErrorException extends ServerException {

    public InternalServerErrorException(String responseBody) {
        super(500, responseBody);
    }
}