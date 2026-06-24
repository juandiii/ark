package xyz.juandiii.ark.async.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.juandiii.ark.async.AsyncArk;
import xyz.juandiii.ark.async.AsyncArkClient;
import xyz.juandiii.ark.async.proxy.AsyncReturnTypeHandler;
import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.http.Transport;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Verifies fluent {@code .raw()} access and proxy {@code RawResponse} return type
 * auto-toggling {@code noThrow} for the async execution model.
 */
@ExtendWith(MockitoExtension.class)
class AsyncRawResponseAccessTest {

    @Mock JsonSerializer serializer;
    @Mock @SuppressWarnings("unchecked")
    Transport<CompletableFuture<RawResponse>> transport;

    interface TypeHelper {
        CompletableFuture<RawResponse> futureRaw();
    }

    private AsyncArk defaultClient() {
        return AsyncArkClient.builder()
                .serializer(serializer)
                .transport(transport)
                .baseUrl("https://api.example.com")
                .build();
    }

    @Test
    void fluentRaw_200_returnsTransportRawResponse() throws Exception {
        RawResponse expected = new RawResponse(200, Map.of(), "ok");
        when(transport.send(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(expected));

        RawResponse raw = defaultClient().get("/foo").retrieve().raw().get();

        assertSame(expected, raw);
        assertEquals(200, raw.statusCode());
    }

    @Test
    void fluentRawWithNoThrow_404_doesNotThrowAndExposesStatusAndBody() throws Exception {
        when(transport.send(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        new RawResponse(404, Map.of(), "{\"error\":\"missing\"}")));

        RawResponse raw = defaultClient().get("/foo").noThrow().retrieve().raw().get();

        assertEquals(404, raw.statusCode());
        assertEquals("{\"error\":\"missing\"}", raw.body());
        assertTrue(raw.isError());
    }

    @Test
    @SuppressWarnings("unchecked")
    void proxyFutureRawResponseReturnType_404_autoNoThrowAndReturnsRaw() throws Exception {
        when(transport.send(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        new RawResponse(404, Map.of(), "{\"err\":\"x\"}")));

        AsyncArk ark = defaultClient();
        AsyncReturnTypeHandler handler = new AsyncReturnTypeHandler();
        Type returnType = TypeHelper.class.getMethod("futureRaw").getGenericReturnType();

        Object result = handler.handle(ark.get("/foo"), returnType);

        assertInstanceOf(CompletableFuture.class, result);
        RawResponse raw = ((CompletableFuture<RawResponse>) result).get();
        assertEquals(404, raw.statusCode());
        assertEquals("{\"err\":\"x\"}", raw.body());
    }
}
