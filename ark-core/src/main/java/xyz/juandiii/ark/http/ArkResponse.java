package xyz.juandiii.ark.http;

import java.util.List;
import java.util.Map;

/**
 * Immutable record representing a typed HTTP response with status and headers.
 *
 * @author Juan Diego Lopez V.
 */
public record ArkResponse<T>(int statusCode, Map<String, List<String>> headers, T body) {

    public boolean isSuccessful() {
        return statusCode >= 200 && statusCode < 300;
    }
}
