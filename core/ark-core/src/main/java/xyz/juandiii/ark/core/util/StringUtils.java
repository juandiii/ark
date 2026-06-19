package xyz.juandiii.ark.core.util;

/**
 * Common string utility methods.
 *
 * @author Juan Diego Lopez V.
 */
public final class StringUtils {

    private StringUtils() {}

    public static boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }

    public static boolean isNotEmpty(String value) {
        return value != null && !value.isEmpty();
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    public static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static String stripTrailingSlash(String value) {
        if (value == null) return null;
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    public static String ensureLeadingSlash(String value) {
        if (value == null || value.isEmpty()) return "/";
        return value.startsWith("/") ? value : "/" + value;
    }
}