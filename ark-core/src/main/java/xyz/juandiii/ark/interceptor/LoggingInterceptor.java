package xyz.juandiii.ark.interceptor;

import xyz.juandiii.ark.AbstractArkBuilder;
import xyz.juandiii.ark.http.RawResponse;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Provides request and response logging interceptors for debugging HTTP calls.
 *
 * <p>Usage:
 * <pre>
 * Ark client = ArkClient.builder()
 *     .requestInterceptor(LoggingInterceptor.request())
 *     .responseInterceptor(LoggingInterceptor.response())
 *     .build();
 * </pre>
 *
 * @author Juan Diego Lopez V.
 */
public final class LoggingInterceptor {

    private static final System.Logger LOGGER = System.getLogger("xyz.juandiii.ark");
    private static final ThreadLocal<Long> REQUEST_START = new ThreadLocal<>();

    private static final String INDENT = "\n    ";

    private LoggingInterceptor() {}

    /**
     * Logging detail level.
     */
    public enum Level {
        OFF,
        BASIC,
        HEADERS,
        BODY
    }

    /**
     * Applies request and response logging to the builder if level is not OFF.
     */
    public static <B extends AbstractArkBuilder<B>> void apply(B builder, Level level) {
        if (level != Level.OFF) {
            builder.requestInterceptor(request(level));
            builder.responseInterceptor(response(level));
        }
    }

    /**
     * Creates a request interceptor with BASIC level.
     */
    public static RequestInterceptor request() {
        return request(Level.BASIC);
    }

    /**
     * Creates a request interceptor with the specified level.
     */
    public static RequestInterceptor request(Level level) {
        return context -> {
            REQUEST_START.set(System.currentTimeMillis());

            StringBuilder sb = new StringBuilder();
            sb.append("--> ").append(context.method()).append(" ").append(buildFullUrl(context));

            if (level.ordinal() >= Level.HEADERS.ordinal()) {
                Map<String, String> headers = context.headers();
                if (headers != null && !headers.isEmpty()) {
                    headers.forEach((key, value) ->
                            sb.append("\n    ").append(key).append(": ").append(value));
                }
            }

            if (level == Level.BODY && context.body() != null) {
                sb.append("\n    ").append(context.body());
            }

            LOGGER.log(System.Logger.Level.DEBUG, sb.toString());
        };
    }

    /**
     * Creates a response interceptor with BASIC level.
     */
    public static ResponseInterceptor response() {
        return response(Level.BASIC);
    }

    /**
     * Creates a response interceptor with the specified level.
     */
    public static ResponseInterceptor response(Level level) {
        return raw -> {
            long duration = 0;
            Long start = REQUEST_START.get();
            if (start != null) {
                duration = System.currentTimeMillis() - start;
                REQUEST_START.remove();
            }

            StringBuilder sb = new StringBuilder();
            sb.append("<-- ").append(raw.statusCode()).append(" (").append(duration).append("ms)");

            if (level.ordinal() >= Level.HEADERS.ordinal()) {
                Map<String, List<String>> headers = raw.headers();
                if (headers != null && !headers.isEmpty()) {
                    headers.forEach((key, values) ->
                            sb.append("\n    ").append(key).append(": ").append(String.join(", ", values)));
                }
            }

            if (level == Level.BODY && raw.body() != null) {
                String body = raw.body();
                if (body.length() > 1024) {
                    sb.append("\n    ").append(body, 0, 1024).append("... (truncated)");
                } else {
                    sb.append("\n    ").append(body);
                }
            }

            System.Logger.Level logLevel = raw.isError()
                    ? System.Logger.Level.WARNING
                    : System.Logger.Level.DEBUG;
            LOGGER.log(logLevel, sb.toString());

            return raw;
        };
    }

    private static String buildFullUrl(RequestContext context) {
        String url = context.path();
        Map<String, String> params = context.queryParams();
        if (params != null && !params.isEmpty()) {
            StringJoiner joiner = new StringJoiner("&");
            params.forEach((k, v) -> joiner.add(k + "=" + v));
            url += "?" + joiner;
        }
        return url;
    }
}
