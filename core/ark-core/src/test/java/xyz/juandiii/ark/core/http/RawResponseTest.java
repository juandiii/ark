package xyz.juandiii.ark.core.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RawResponseTest {

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 500, 502, 503})
    void isErrorStatusForErrorCodes(int statusCode) {
        assertTrue(RawResponse.isErrorStatus(statusCode));
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 201, 204, 301, 302, 399, 600, 999})
    void isNotErrorStatusForNonErrorCodes(int statusCode) {
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

    @Test
    void header_matchesCaseInsensitively() {
        RawResponse raw = new RawResponse(200, Map.of("etag", List.of("\"abc\"")), "");
        assertEquals(Optional.of("\"abc\""), raw.header("ETag"));
    }

    @Test
    void header_returnsFirstValue() {
        RawResponse raw = new RawResponse(200, Map.of("X-Multi", List.of("a", "b")), "");
        assertEquals(Optional.of("a"), raw.header("X-Multi"));
    }

    @Test
    void header_absent_returnsEmpty() {
        RawResponse raw = new RawResponse(200, Map.of("X-Other", List.of("a")), "");
        assertTrue(raw.header("ETag").isEmpty());
    }

    @Test
    void header_nullHeadersMap_returnsEmpty() {
        assertTrue(new RawResponse(200, null, "x").header("A").isEmpty());
    }

    @Test
    void ofBytes_preservesBinaryBytes() {
        byte[] input = {(byte) 0x50, (byte) 0x4B, (byte) 0x03, (byte) 0x04, (byte) 0xFF, (byte) 0xFE};
        RawResponse raw = RawResponse.ofBytes(200, Map.of(), input);
        assertArrayEquals(input, raw.bodyBytes());
    }

    @Test
    void ofBytes_decodesBodyWithContentTypeCharset() {
        byte[] b = "ñ".getBytes(StandardCharsets.ISO_8859_1);
        RawResponse raw = RawResponse.ofBytes(200, Map.of("content-type", List.of("text/plain; charset=ISO-8859-1")), b);
        assertEquals("ñ", raw.body());
    }

    @Test
    void ofBytes_quotedCharset() {
        byte[] b = "ñ".getBytes(StandardCharsets.ISO_8859_1);
        RawResponse raw = RawResponse.ofBytes(200, Map.of("Content-Type", List.of("text/plain; charset=\"ISO-8859-1\"")), b);
        assertEquals("ñ", raw.body());
    }

    @Test
    void ofBytes_defaultsToUtf8() {
        RawResponse raw = RawResponse.ofBytes(200, Map.of(), "ñ".getBytes(StandardCharsets.UTF_8));
        assertEquals("ñ", raw.body());
    }

    @Test
    void ofBytes_invalidCharset_fallsBackToUtf8() {
        RawResponse raw = RawResponse.ofBytes(200, Map.of("Content-Type", List.of("text/plain; charset=bogus-xyz")),
                "ñ".getBytes(StandardCharsets.UTF_8));
        assertEquals("ñ", raw.body());
    }

    @Test
    void ofBytes_nullBytes_givesEmpty() {
        RawResponse raw = RawResponse.ofBytes(200, Map.of(), null);
        assertEquals(0, raw.bodyBytes().length);
        assertEquals("", raw.body());
    }

    @Test
    void textConstructor_bodyBytesFallsBackToUtf8() {
        RawResponse raw = new RawResponse(200, Map.of(), "ñ");
        assertArrayEquals("ñ".getBytes(StandardCharsets.UTF_8), raw.bodyBytes());
    }

    @Test
    void textConstructor_nullBody_bodyBytesNull() {
        assertNull(new RawResponse(200, Map.of(), null).bodyBytes());
    }
}
