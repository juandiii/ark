package xyz.juandiii.ark.core.http;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpTransportTest {

    @Test
    void givenTransportWithoutBinaryOverride_whenSendBinary_thenThrowsUnsupportedOperation() {
        HttpTransport transport = (method, uri, headers, body, timeout) ->
                new RawResponse(200, Map.of(), "");

        assertThrows(UnsupportedOperationException.class, () ->
                transport.sendBinary("POST", URI.create("http://x"), Map.of(), new byte[]{1, 2, 3}, null));
    }
}
