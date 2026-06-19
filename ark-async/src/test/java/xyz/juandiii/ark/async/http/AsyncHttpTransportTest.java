package xyz.juandiii.ark.async.http;

import org.junit.jupiter.api.Test;
import xyz.juandiii.ark.core.http.RawResponse;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AsyncHttpTransportTest {

    @Test
    void givenTransportWithoutBinaryOverride_whenSendBinaryAsync_thenThrowsUnsupportedOperation() {
        AsyncHttpTransport transport = (method, uri, headers, body, timeout) ->
                CompletableFuture.completedFuture(new RawResponse(200, Map.of(), ""));

        assertThrows(UnsupportedOperationException.class, () ->
                transport.sendBinaryAsync("POST", URI.create("http://x"), Map.of(), new byte[]{1}, null));
    }
}
