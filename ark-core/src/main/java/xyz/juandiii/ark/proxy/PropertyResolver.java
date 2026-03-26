package xyz.juandiii.ark.proxy;

import xyz.juandiii.ark.exceptions.ArkException;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves property placeholders in the form ${key} or ${key:default}.
 *
 * @author Juan Diego Lopez V.
 */
public final class PropertyResolver {

    private static final Pattern PLACEHOLDER = Pattern.compile("^\\$\\{([^}]+)}$");

    private PropertyResolver() {}

    /**
     * Resolves a value that may contain a property placeholder.
     *
     * @param value the raw value (e.g., "${api.url}" or "https://example.com")
     * @param propertyLookup function to resolve property keys
     * @return the resolved value
     */
    public static String resolve(String value, Function<String, String> propertyLookup) {
        if (value == null || value.isEmpty()) return value;

        Matcher m = PLACEHOLDER.matcher(value);
        if (!m.matches()) return value;

        String key = m.group(1);
        String[] parts = key.split(":", 2);
        String resolved = propertyLookup.apply(parts[0]);
        if (resolved != null) return resolved;
        if (parts.length > 1) return parts[1];
        throw new ArkException("Property not found: " + parts[0]);
    }
}
