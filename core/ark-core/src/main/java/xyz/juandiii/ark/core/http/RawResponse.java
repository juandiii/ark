package xyz.juandiii.ark.core.http;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    /**
     * Returns the first value of the given response header, matching the name
     * case-insensitively (HTTP header names are case-insensitive per RFC 9110, and
     * transports differ in how they store them).
     *
     * @param name header name, e.g. {@code "ETag"}
     * @return the first value, or empty if the header is absent or {@link #headers()} is {@code null}
     */
    public Optional<String> header(String name) {
        if (headers == null || name == null) return Optional.empty();
        for (Map.Entry<String, List<String>> e : headers.entrySet()) {
            if (name.equalsIgnoreCase(e.getKey()) && e.getValue() != null && !e.getValue().isEmpty()) {
                return Optional.of(e.getValue().get(0));
            }
        }
        return Optional.empty();
    }
}
