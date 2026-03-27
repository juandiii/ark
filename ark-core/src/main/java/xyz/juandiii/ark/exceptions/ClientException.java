package xyz.juandiii.ark.exceptions;

/**
 * Exception for HTTP 4xx client errors.
 *
 * @author Juan Diego Lopez V.
 */
public class ClientException extends ApiException {

    public ClientException(int statusCode, String responseBody) {
        super(statusCode, responseBody);
    }
}