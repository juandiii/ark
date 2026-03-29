package xyz.juandiii.ark.core.exceptions;

/**
 * Exception for HTTP 5xx server errors.
 *
 * @author Juan Diego Lopez V.
 */
public class ServerException extends ApiException {

    public ServerException(int statusCode, String responseBody) {
        super(statusCode, responseBody);
    }
}