package xyz.juandiii.ark.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RawResponseTest {

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 500, 502, 503})
    void isErrorStatusForErrorCodes(int statusCode) {
        assertTrue(RawResponse.isErrorStatus(statusCode));
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 201, 204, 301, 302, 399})
    void isNotErrorStatusForSuccessCodes(int statusCode) {
        assertFalse(RawResponse.isErrorStatus(statusCode));
    }

    @Test
    void isErrorOnInstance() {
        assertTrue(new RawResponse(500, Map.of(), "error").isError());
        assertFalse(new RawResponse(200, Map.of(), "ok").isError());
    }

    @Test
    void recordAccessors() {
        RawResponse raw = new RawResponse(200, Map.of("Content-Type", List.of("application/json")), "body");
        assertEquals(200, raw.statusCode());
        assertEquals("body", raw.body());
        assertTrue(raw.headers().containsKey("Content-Type"));
    }
}