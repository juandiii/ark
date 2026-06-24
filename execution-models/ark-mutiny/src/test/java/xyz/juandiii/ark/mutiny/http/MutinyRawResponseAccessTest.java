package xyz.juandiii.ark.mutiny.http;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.mutiny.MutinyArk;
import xyz.juandiii.ark.mutiny.MutinyArkClient;
import xyz.juandiii.ark.mutiny.proxy.MutinyReturnTypeHandler;

import java.lang.reflect.Type;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Verifies fluent {@code .raw()} access and proxy {@code RawResponse} return type
 * auto-toggling {@code noThrow} for the Mutiny execution model.
 */
@ExtendWith(MockitoExtension.class)
class MutinyRawResponseAccessTest {

    @Mock JsonSerializer serializer;
    @Mock MutinyHttpTransport transport;

    interface TypeHelper {
        Uni<RawResponse> uniRaw();
    }

    private MutinyArk defaultClient() {
        return MutinyArkClient.builder()
                .serializer(serializer)
                .transport(transport)
                .baseUrl("https://api.example.com")
                .build();
    }

    @Test
    void fluentRaw_200_returnsTransportRawResponse() {
        RawResponse expected = new RawResponse(200, Map.of(), "ok");
        when(transport.send(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(Uni.createFrom().item(expected));

        RawResponse raw = defaultClient().get("/foo").retrieve().raw().await().indefinitely();

        assertSame(expected, raw);
        assertEquals(200, raw.statusCode());
    }

    @Test
    void fluentRawWithNoThrow_404_doesNotThrowAndExposesStatusAndBody() {
        when(transport.send(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(Uni.createFrom().item(new RawResponse(404, Map.of(), "{\"error\":\"missing\"}")));

        RawResponse raw = defaultClient().get("/foo").noThrow().retrieve().raw().await().indefinitely();

        assertEquals(404, raw.statusCode());
        assertEquals("{\"error\":\"missing\"}", raw.body());
        assertTrue(raw.isError());
    }

    @Test
    @SuppressWarnings("unchecked")
    void proxyUniRawResponseReturnType_404_autoNoThrowAndReturnsRaw() throws Exception {
        when(transport.send(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(Uni.createFrom().item(new RawResponse(404, Map.of(), "{\"err\":\"x\"}")));

        MutinyArk ark = defaultClient();
        MutinyReturnTypeHandler handler = new MutinyReturnTypeHandler();
        Type returnType = TypeHelper.class.getMethod("uniRaw").getGenericReturnType();

        Object result = handler.handle(ark.get("/foo"), returnType);

        assertInstanceOf(Uni.class, result);
        RawResponse raw = ((Uni<RawResponse>) result).await().indefinitely();
        assertEquals(404, raw.statusCode());
        assertEquals("{\"err\":\"x\"}", raw.body());
    }
}
