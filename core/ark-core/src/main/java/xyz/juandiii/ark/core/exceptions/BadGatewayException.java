package xyz.juandiii.ark.core.exceptions;

/**
 * Exception for HTTP 502 Bad Gateway.
 *
 * @author Juan Diego Lopez V.
 */
public class BadGatewayException extends ServerException {

    public BadGatewayException(String responseBody) {
        super(502, responseBody);
    }
}