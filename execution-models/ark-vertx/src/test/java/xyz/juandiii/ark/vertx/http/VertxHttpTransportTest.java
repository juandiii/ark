package xyz.juandiii.ark.vertx.http;

import io.vertx.core.Future;
import org.junit.jupiter.api.Test;
import xyz.juandiii.ark.core.http.RawResponse;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

class VertxHttpTransportTest {

    @Test
    void givenTransportWithoutBinaryOverride_whenSendBinary_thenThrowsUnsupportedOperation() {
        VertxHttpTransport transport = (method, uri, headers, body, timeout) ->
                Future.succeededFuture(new RawResponse(200, Map.of(), ""));

        assertThrows(UnsupportedOperationException.class, () ->
                transport.sendBinary("POST", URI.create("http://x"), Map.of(), new byte[]{1}, null));
    }
}
