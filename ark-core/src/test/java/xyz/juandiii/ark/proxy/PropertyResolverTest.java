package xyz.juandiii.ark.proxy;

import org.junit.jupiter.api.Test;
import xyz.juandiii.ark.exceptions.ArkException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PropertyResolverTest {

    private static final Map<String, String> PROPS = Map.of(
            "api.url", "https://api.example.com",
            "api.port", "8080"
    );

    @Test
    void givenDirectValue_thenReturnsAsIs() {
        assertEquals("https://example.com",
                PropertyResolver.resolve("https://example.com", PROPS::get));
    }

    @Test
    void givenPlaceholder_thenResolvesFromProperties() {
        assertEquals("https://api.example.com",
                PropertyResolver.resolve("${api.url}", PROPS::get));
    }

    @Test
    void givenPlaceholderWithDefault_thenUsesDefault() {
        assertEquals("https://fallback.com",
                PropertyResolver.resolve("${missing.key:https://fallback.com}", PROPS::get));
    }

    @Test
    void givenPlaceholderWithDefaultAndKeyExists_thenUsesProperty() {
        assertEquals("https://api.example.com",
                PropertyResolver.resolve("${api.url:https://fallback.com}", PROPS::get));
    }

    @Test
    void givenMissingProperty_thenThrowsArkException() {
        assertThrows(ArkException.class,
                () -> PropertyResolver.resolve("${missing.key}", PROPS::get));
    }

    @Test
    void givenEmptyValue_thenReturnsEmpty() {
        assertEquals("", PropertyResolver.resolve("", PROPS::get));
    }

    @Test
    void givenNull_thenReturnsNull() {
        assertNull(PropertyResolver.resolve(null, PROPS::get));
    }
}
