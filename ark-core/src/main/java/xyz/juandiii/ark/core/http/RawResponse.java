package xyz.juandiii.ark.core.http;

import java.util.List;
import java.util.Map;

/**
 * Immutable record representing a raw HTTP response.
 *
 * @author Juan Diego Lopez V.
 */
public record RawResponse(int statusCode, Map<String, List<String>> headers, String body) {

    public static boolean isErrorStatus(int statusCode) {
        return statusCode >= 400 && statusCode <= 599;
    }

    public boolean isError() {
        return isErrorStatus(statusCode);
    }
}
