package xyz.juandiii.ark.core.exceptions;

/**
 * Exception for HTTP 429 Too Many Requests.
 *
 * @author Juan Diego Lopez V.
 */
public class TooManyRequestsException extends ClientException {

    public TooManyRequestsException(String responseBody) {
        super(429, responseBody);
    }
}