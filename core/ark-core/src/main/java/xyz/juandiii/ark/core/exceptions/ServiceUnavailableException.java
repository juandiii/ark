package xyz.juandiii.ark.core.exceptions;

/**
 * Exception for HTTP 503 Service Unavailable.
 *
 * @author Juan Diego Lopez V.
 */
public class ServiceUnavailableException extends ServerException {

    public ServiceUnavailableException(String responseBody) {
        super(503, responseBody);
    }
}