package xyz.juandiii.ark.vertx.http;

import io.vertx.core.Future;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.vertx.VertxArk;
import xyz.juandiii.ark.vertx.VertxArkClient;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Verifies fluent {@code .raw()} access for the Vert.x execution model.
 * Vert.x has no proxy return-type handler yet, so only fluent paths are covered.
 */
@ExtendWith(MockitoExtension.class)
class VertxRawResponseAccessTest {

    @Mock JsonSerializer serializer;
    @Mock VertxHttpTransport transport;

    private VertxArk defaultClient() {
        return VertxArkClient.builder()
                .serializer(serializer)
                .transport(transport)
                .baseUrl("https://api.example.com")
                .build();
    }

    @Test
    void fluentRaw_200_returnsTransportRawResponse() throws Exception {
        RawResponse expected = new RawResponse(200, Map.of(), "ok");
        when(transport.send(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(Future.succeededFuture(expected));

        RawResponse raw = await(defaultClient().get("/foo").retrieve().raw());

        assertSame(expected, raw);
        assertEquals(200, raw.statusCode());
    }

    @Test
    void fluentRawWithNoThrow_404_doesNotThrowAndExposesStatusAndBody() throws Exception {
        when(transport.send(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(Future.succeededFuture(new RawResponse(404, Map.of(), "{\"error\":\"missing\"}")));

        RawResponse raw = await(defaultClient().get("/foo").noThrow().retrieve().raw());

        assertEquals(404, raw.statusCode());
        assertEquals("{\"error\":\"missing\"}", raw.body());
        assertTrue(raw.isError());
    }

    @Test
    void fluentRawWithClientThrowOnErrorFalse_500_exposesRawBody() throws Exception {
        when(transport.send(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(Future.succeededFuture(new RawResponse(500, Map.of(), "boom")));

        VertxArk ark = VertxArkClient.builder()
                .serializer(serializer)
                .transport(transport)
                .baseUrl("https://api.example.com")
                .throwOnError(false)
                .build();

        RawResponse raw = await(ark.get("/foo").retrieve().raw());

        assertEquals(500, raw.statusCode());
        assertEquals("boom", raw.body());
        assertTrue(raw.isError());
    }

    private <T> T await(Future<T> future) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<T> value = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        future.onComplete(ar -> {
            if (ar.succeeded()) value.set(ar.result());
            else error.set(ar.cause());
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        if (error.get() != null) throw new RuntimeException(error.get());
        return value.get();
    }
}
