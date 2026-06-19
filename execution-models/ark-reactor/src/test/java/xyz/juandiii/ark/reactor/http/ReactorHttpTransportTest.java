package xyz.juandiii.ark.reactor.http;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import xyz.juandiii.ark.core.http.RawResponse;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ReactorHttpTransportTest {

    @Test
    void givenTransportWithoutBinaryOverride_whenSendBinary_thenThrowsUnsupportedOperation() {
        ReactorHttpTransport transport = (method, uri, headers, body, timeout) ->
                Mono.just(new RawResponse(200, Map.of(), ""));

        assertThrows(UnsupportedOperationException.class, () ->
                transport.sendBinary("POST", URI.create("http://x"), Map.of(), new byte[]{1}, null));
    }
}
