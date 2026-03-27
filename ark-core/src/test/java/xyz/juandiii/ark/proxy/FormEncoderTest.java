package xyz.juandiii.ark.proxy;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FormEncoderTest {

    @Test
    void givenParams_thenEncodesAsFormUrlEncoded() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("grant_type", "client_credentials");
        params.put("client_id", "myapp");

        assertEquals("grant_type=client_credentials&client_id=myapp", FormEncoder.encode(params));
    }

    @Test
    void givenSpecialChars_thenUrlEncodes() {
        Map<String, String> params = Map.of("key", "hello world&more=yes");

        String result = FormEncoder.encode(params);
        assertTrue(result.contains("hello+world"));
        assertTrue(result.contains("%26"));
    }

    @Test
    void givenEmptyParams_thenReturnsEmpty() {
        assertEquals("", FormEncoder.encode(Map.of()));
    }

    @Test
    void givenUrlEncode_thenEncodesValue() {
        assertEquals("hello+world", FormEncoder.urlEncode("hello world"));
        assertEquals("a%26b", FormEncoder.urlEncode("a&b"));
    }
}