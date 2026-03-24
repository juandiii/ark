package xyz.juandiii.ark.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ArkResponseTest {

    @ParameterizedTest
    @ValueSource(ints = {200, 201, 204, 299})
    void isSuccessfulFor2xx(int statusCode) {
        assertTrue(new ArkResponse<>(statusCode, Map.of(), "ok").isSuccessful());
    }

    @ParameterizedTest
    @ValueSource(ints = {100, 301, 400, 404, 500})
    void isNotSuccessfulForNon2xx(int statusCode) {
        assertFalse(new ArkResponse<>(statusCode, Map.of(), "fail").isSuccessful());
    }

    @Test
    void recordAccessors() {
        ArkResponse<String> response = new ArkResponse<>(200, Map.of(), "hello");
        assertEquals(200, response.statusCode());
        assertEquals("hello", response.body());
        assertNotNull(response.headers());
    }
}