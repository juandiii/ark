package xyz.juandiii.ark.core.http;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable raw HTTP response as produced by an {@link HttpTransport}. Contains
 * the status code, the response headers (multi-valued), and the response body
 * both as text via {@link #body()} and as the original bytes via {@link #bodyBytes()}.
 *
 * @param statusCode HTTP status code
 * @param headers    response headers (may be empty, may be {@code null} for some transports)
 * @param body       response body as text (may be {@code null} or empty)
 * @param bodyBytes  original response bytes (may be {@code null} when built from text only)
 *
 * @author Juan Diego Lopez V.
 */
public record RawResponse(int statusCode, Map<String, List<String>> headers, String body, byte[] bodyBytes) {

    /** Text-only constructor (kept for backward compatibility); {@link #bodyBytes()} falls back to UTF-8 of {@code body}. */
    public RawResponse(int statusCode, Map<String, List<String>> headers, String body) {
        this(statusCode, headers, body, null);
    }

    /**
     * Build from the raw bytes received on the wire; {@link #body()} is decoded using the
     * {@code charset} parameter of {@code Content-Type}, falling back to UTF-8.
     *
     * @param statusCode HTTP status code
     * @param headers    response headers
     * @param bytes      response bytes (may be {@code null}, treated as empty)
     * @return a response carrying both the bytes and their decoded text
     */
    public static RawResponse ofBytes(int statusCode, Map<String, List<String>> headers, byte[] bytes) {
        byte[] b = bytes != null ? bytes : new byte[0];
        return new RawResponse(statusCode, headers, new String(b, charsetOf(headers)), b);
    }

    /**
     * Original response bytes. Never mutate the returned array. When this instance was created
     * via the text-only constructor, returns {@code body} encoded as UTF-8 (or {@code null} if body is null).
     *
     * @return the response bytes
     */
    @Override
    public byte[] bodyBytes() {
        if (bodyBytes != null) return bodyBytes;
        return body != null ? body.getBytes(StandardCharsets.UTF_8) : null;
    }

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
        return firstHeader(headers, name);
    }

    static Optional<String> firstHeader(Map<String, List<String>> headers, String name) {
        if (headers == null || name == null) return Optional.empty();
        for (Map.Entry<String, List<String>> e : headers.entrySet()) {
            if (name.equalsIgnoreCase(e.getKey()) && e.getValue() != null && !e.getValue().isEmpty()) {
                return Optional.of(e.getValue().get(0));
            }
        }
        return Optional.empty();
    }

    private static Charset charsetOf(Map<String, List<String>> headers) {
        String contentType = firstHeader(headers, "Content-Type").orElse(null);
        if (contentType == null) return StandardCharsets.UTF_8;
        for (String part : contentType.split(";")) {
            String p = part.trim();
            if (p.regionMatches(true, 0, "charset=", 0, 8)) {
                String v = p.substring(8).trim();
                if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) {
                    v = v.substring(1, v.length() - 1);
                }
                try {
                    return Charset.forName(v);
                } catch (IllegalArgumentException e) {
                    return StandardCharsets.UTF_8;
                }
            }
        }
        return StandardCharsets.UTF_8;
    }
}
