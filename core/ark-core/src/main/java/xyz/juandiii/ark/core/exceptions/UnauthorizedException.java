package xyz.juandiii.ark.core.exceptions;

/**
 * Exception for HTTP 401 Unauthorized.
 *
 * @author Juan Diego Lopez V.
 */
public class UnauthorizedException extends ClientException {

    public UnauthorizedException(String responseBody) {
        super(401, responseBody);
    }
}