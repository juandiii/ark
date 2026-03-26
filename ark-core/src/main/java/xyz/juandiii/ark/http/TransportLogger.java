package xyz.juandiii.ark.http;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Shared transport-level logging utility.
 *
 * @author Juan Diego Lopez V.
 */
public final class TransportLogger {

    private static final String INDENT = "\n    ";

    private TransportLogger() {}

    public static String formatRequest(String method, URI uri, Map<String, String> headers, String body) {
        StringBuilder sb = new StringBuilder();
        sb.append("--> REQUEST").append(INDENT)
          .append("Method: ").append(method).append(INDENT)
          .append("URL: ").append(uri).append(INDENT)
          .append("Scheme: ").append(uri.getScheme()).append(INDENT)
          .append("Host: ").append(uri.getHost()).append(INDENT)
          .append("Port: ").append(uri.getPort()).append(INDENT)
          .append("Path: ").append(uri.getPath() != null ? uri.getPath() : "/").append(INDENT)
          .append("Query: ").append(uri.getQuery() != null ? uri.getQuery() : "none");
        if (!headers.isEmpty()) {
            sb.append(INDENT).append("Headers:");
            headers.forEach((k, v) -> sb.append(INDENT).append("  ").append(k).append(": ").append(v));
        }
        if (body != null) {
            sb.append(INDENT).append("Body: ").append(body);
        }
        return sb.toString();
    }

    public static String formatResponse(int statusCode, Map<String, List<String>> headers, String body) {
        StringBuilder sb = new StringBuilder();
        sb.append("<-- RESPONSE").append(INDENT)
          .append("Status: ").append(statusCode);
        if (headers != null && !headers.isEmpty()) {
            sb.append(INDENT).append("Headers:");
            headers.forEach((k, values) ->
                    sb.append(INDENT).append("  ").append(k).append(": ").append(String.join(", ", values)));
        }
        if (body != null) {
            sb.append(INDENT).append("Body: ").append(body);
        }
        return sb.toString();
    }
}
