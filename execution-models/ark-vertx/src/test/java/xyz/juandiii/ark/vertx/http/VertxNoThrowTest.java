package xyz.juandiii.ark.vertx.http;

import io.vertx.core.Future;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.core.exceptions.NotFoundException;
import xyz.juandiii.ark.core.http.ArkResponse;
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
 * Verifies the {@code noThrow()} per-request opt-out and the
 * client-level {@code throwOnError(false)} default for the Vert.x execution model.
 */
@ExtendWith(MockitoExtension.class)
class VertxNoThrowTest {

    @Mock
    JsonSerializer serializer;

    @Mock
    VertxHttpTransport transport;

    private VertxArk client(boolean throwOnError) {
        return VertxArkClient.builder()
                .serializer(serializer)
                .transport(transport)
                .baseUrl("https://api.example.com")
                .throwOnError(throwOnError)
                .build();
    }

    private VertxArk defaultClient() {
        return VertxArkClient.builder()
                .serializer(serializer)
                .transport(transport)
                .baseUrl("https://api.example.com")
                .build();
    }

    @Test
    void defaultBehavior_404_futureFailsWithNotFoundException() throws Exception {
        when(transport.send(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(Future.succeededFuture(new RawResponse(404, Map.of(), "{\"error\":\"not found\"}")));

        VertxArk ark = defaultClient();
        Future<ArkResponse<String>> future = ark.get("/users/1").retrieve().toEntity(String.class);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        future.onComplete(ar -> {
            if (ar.failed()) error.set(ar.cause());
            latch.countDown();
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertInstanceOf(NotFoundException.class, error.get());
    }

    @Test
    void perRequestNoThrow_404_returnsResponseWithStatus404() throws Exception {
        when(transport.send(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(Future.succeededFuture(new RawResponse(404, Map.of(), "{\"error\":\"not found\"}")));

        VertxArk ark = defaultClient();
        ArkResponse<String> response = await(ark.get("/users/1").noThrow().retrieve().toEntity(String.class));

        assertEquals(404, response.statusCode());
        assertFalse(response.isSuccessful());
    }

    @Test
    void clientLevelThrowOnErrorFalse_404_isPermissive() throws Exception {
        when(transport.send(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(Future.succeededFuture(new RawResponse(404, Map.of(), "{\"error\":\"not found\"}")));

        VertxArk ark = client(false);
        ArkResponse<String> response = await(ark.get("/users/1").retrieve().toEntity(String.class));

        assertEquals(404, response.statusCode());
        assertFalse(response.isSuccessful());
    }

    @Test
    void clientLevelThrowOnErrorFalse_withoutNoThrow_stillPermissive() throws Exception {
        when(transport.send(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(Future.succeededFuture(new RawResponse(500, Map.of(), "boom")));

        VertxArk ark = client(false);
        ArkResponse<String> response = await(ark.get("/users/1").retrieve().toEntity(String.class));

        assertEquals(500, response.statusCode());
        assertFalse(response.isSuccessful());
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
