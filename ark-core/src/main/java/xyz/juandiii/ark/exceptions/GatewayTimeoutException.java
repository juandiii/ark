package xyz.juandiii.ark.exceptions;

/**
 * Exception for HTTP 504 Gateway Timeout.
 *
 * @author Juan Diego Lopez V.
 */
public class GatewayTimeoutException extends ServerException {

    public GatewayTimeoutException(String responseBody) {
        super(504, responseBody);
    }
}