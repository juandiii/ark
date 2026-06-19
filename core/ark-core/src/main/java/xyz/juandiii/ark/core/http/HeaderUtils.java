package xyz.juandiii.ark.core.http;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility for converting transport-specific header types to the standard map format.
 *
 * @author Juan Diego Lopez V.
 */
public final class HeaderUtils {

    private HeaderUtils() {}

    /**
     * Converts an iterable of key-value entries to a multi-value header map.
     */
    public static Map<String, List<String>> toHeaderMap(Iterable<Map.Entry<String, String>> entries) {
        Map<String, List<String>> map = new LinkedHashMap<>();
        entries.forEach(e -> map.computeIfAbsent(e.getKey(), k -> new ArrayList<>()).add(e.getValue()));
        return map;
    }
}
