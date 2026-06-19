package xyz.juandiii.ark.core.http;

import java.util.List;
import java.util.Map;

/**
 * Immutable raw HTTP response as produced by an {@link HttpTransport}. Contains
 * the status code, the response headers (multi-valued), and the response body
 * as a text string. Binary payloads are not represented here — transports
 * decode bytes to UTF-8 before constructing this record.
 *
 * @param statusCode HTTP status code
 * @param headers    response headers (may be empty, may be {@code null} for some transports)
 * @param body       response body as text (may be {@code null} or empty)
 *
 * @author Juan Diego Lopez V.
 */
public record RawResponse(int statusCode, Map<String, List<String>> headers, String body) {

    /**
     * @param statusCode HTTP status code
     * @return {@code true} if {@code statusCode} is in the {@code 4xx} or {@code 5xx} range
     */
    public static boolean isErrorStatus(int statusCode) {
        return statusCode >= 400 && statusCode <= 599;
    }

    /**
     * @return {@code true} if this response's status code is an HTTP error (4xx or 5xx)
     */
    public boolean isError() {
        return isErrorStatus(statusCode);
    }
}
