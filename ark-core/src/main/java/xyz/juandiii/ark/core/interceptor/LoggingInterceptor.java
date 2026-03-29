package xyz.juandiii.ark.core.interceptor;

import xyz.juandiii.ark.core.AbstractArkBuilder;
import xyz.juandiii.ark.core.http.RawResponse;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Provides paired request/response logging interceptors with timing.
 * Use {@link #apply(AbstractArkBuilder, Level)} to add logging to a builder.
 *
 * @author Juan Diego Lopez V.
 */
public final class LoggingInterceptor {

    private static final System.Logger LOGGER = System.getLogger("xyz.juandiii.ark");

    private LoggingInterceptor() {}

    public enum Level {
        NONE,
        BASIC,
        HEADERS,
        BODY
    }

    /**
     * Parses a logging level from a string. Returns NONE if null or invalid.
     */
    public static Level parseLevel(String value) {
        if (value == null) return Level.NONE;
        try {
            return Level.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Level.NONE;
        }
    }

    /**
     * Applies paired request/response logging to the builder with correct timing.
     */
    private static final ThreadLocal<Long> REQUEST_START = new ThreadLocal<>();

    public static <B extends AbstractArkBuilder<B>> void apply(B builder, Level level) {
        if (level == Level.NONE) return;
        builder.requestInterceptor(context -> {
            REQUEST_START.set(System.currentTimeMillis());
            logRequest(context, level);
        });
        builder.responseInterceptor(raw -> {
            Long start = REQUEST_START.get();
            long duration = start != null ? System.currentTimeMillis() - start : -1;
            REQUEST_START.remove();
            logResponse(raw, level, duration);
            return raw;
        });
    }

    private static void logRequest(RequestContext context, Level level) {
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
    }

    private static void logResponse(RawResponse raw, Level level, long durationMs) {
        StringBuilder sb = new StringBuilder();
        sb.append("<-- ").append(raw.statusCode());
        if (durationMs >= 0) {
            sb.append(" (").append(durationMs).append("ms)");
        }

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
    }

    private static String buildFullUrl(RequestContext context) {
        String url = context.path();
        Map<String, String> params = context.queryParams();
        if (params != null && !params.isEmpty()) {
            StringJoiner joiner = new StringJoiner("&");
            params.forEach((k, v) -> joiner.add(
                    URLEncoder.encode(k, StandardCharsets.UTF_8) + "="
                            + URLEncoder.encode(v, StandardCharsets.UTF_8)));
            url += "?" + joiner;
        }
        return url;
    }
}
