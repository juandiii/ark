package xyz.juandiii.ark.mutiny.http;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import xyz.juandiii.ark.core.http.RawResponse;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

class MutinyHttpTransportTest {

    @Test
    void givenTransportWithoutBinaryOverride_whenSendBinary_thenThrowsUnsupportedOperation() {
        MutinyHttpTransport transport = (method, uri, headers, body, timeout) ->
                Uni.createFrom().item(new RawResponse(200, Map.of(), ""));

        assertThrows(UnsupportedOperationException.class, () ->
                transport.sendBinary("POST", URI.create("http://x"), Map.of(), new byte[]{1}, null));
    }
}
