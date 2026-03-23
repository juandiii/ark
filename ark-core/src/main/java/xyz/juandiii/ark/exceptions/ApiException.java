package xyz.juandiii.ark.exceptions;

/**
 * Exception thrown for HTTP error responses (status >= 400).
 *
 * @author Juan Diego Lopez V.
 */
public class ApiException extends RuntimeException {

    private final int statusCode;
    private final String responseBody;

    public ApiException(int statusCode, String responseBody) {
        super("HTTP " + statusCode + ": " + responseBody);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int statusCode() {
        return statusCode;
    }

    public String responseBody() {
        return responseBody;
    }

    public boolean isUnauthorized() {
        return statusCode == 401;
    }

    public boolean isNotFound() {
        return statusCode == 404;
    }
}