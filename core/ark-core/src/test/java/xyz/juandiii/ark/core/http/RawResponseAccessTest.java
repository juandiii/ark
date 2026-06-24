package xyz.juandiii.ark.core.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.juandiii.ark.core.Ark;
import xyz.juandiii.ark.core.ArkClient;
import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.core.proxy.SyncReturnTypeHandler;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Verifies fluent {@code .raw()} access and proxy {@code RawResponse} return type
 * auto-toggling {@code noThrow} for the sync execution model.
 */
@ExtendWith(MockitoExtension.class)
class RawResponseAccessTest {

    @Mock JsonSerializer serializer;
    @Mock HttpTransport transport;

    private Ark defaultClient() {
        return ArkClient.builder()
                .serializer(serializer)
                .transport(transport)
                .baseUrl("https://api.example.com")
                .build();
    }

    @Test
    void fluentRaw_200_returnsTransportRawResponse() {
        RawResponse expected = new RawResponse(200, Map.of("X-Trace", java.util.List.of("abc")), "{\"x\":1}");
        when(transport.send(anyString(), any(), anyMap(), any(), any())).thenReturn(expected);

        RawResponse raw = defaultClient().get("/foo").retrieve().raw();

        assertSame(expected, raw);
        assertEquals(200, raw.statusCode());
        assertEquals("{\"x\":1}", raw.body());
    }

    @Test
    void fluentRawWithNoThrow_404_doesNotThrowAndExposesStatusAndBody() {
        when(transport.send(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(new RawResponse(404, Map.of(), "{\"error\":\"missing\"}"));

        RawResponse raw = defaultClient().get("/foo").noThrow().retrieve().raw();

        assertEquals(404, raw.statusCode());
        assertEquals("{\"error\":\"missing\"}", raw.body());
        assertTrue(raw.isError());
    }

    @Test
    void proxyRawResponseReturnType_404_autoNoThrowAndReturnsRaw() throws Exception {
        when(transport.send(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(new RawResponse(404, Map.of(), "{\"err\":\"x\"}"));

        Ark ark = defaultClient();
        SyncReturnTypeHandler handler = new SyncReturnTypeHandler();
        java.lang.reflect.Type returnType = RawResponse.class;
        Object result = handler.handle(ark.get("/foo"), returnType);

        assertInstanceOf(RawResponse.class, result);
        RawResponse raw = (RawResponse) result;
        assertEquals(404, raw.statusCode());
        assertEquals("{\"err\":\"x\"}", raw.body());
    }
}
