package xyz.juandiii.ark.http;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HeaderUtilsTest {

    @Test
    void givenEntries_thenConvertsToMultiValueMap() {
        var entries = List.of(
                Map.entry("Content-Type", "application/json"),
                Map.entry("Accept", "text/html"),
                Map.entry("Accept", "application/json")
        );

        Map<String, List<String>> result = HeaderUtils.toHeaderMap(entries);

        assertEquals(List.of("application/json"), result.get("Content-Type"));
        assertEquals(List.of("text/html", "application/json"), result.get("Accept"));
    }

    @Test
    void givenEmptyEntries_thenReturnsEmptyMap() {
        Map<String, List<String>> result = HeaderUtils.toHeaderMap(List.of());
        assertTrue(result.isEmpty());
    }
}
