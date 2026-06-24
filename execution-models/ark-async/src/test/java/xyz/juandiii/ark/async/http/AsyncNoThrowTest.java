package xyz.juandiii.ark.async.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.juandiii.ark.async.AsyncArk;
import xyz.juandiii.ark.async.AsyncArkClient;
import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.core.exceptions.NotFoundException;
import xyz.juandiii.ark.core.http.ArkResponse;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.http.Transport;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Verifies the {@code noThrow()} per-request opt-out and the
 * client-level {@code throwOnError(false)} default for the async execution model.
 */
@ExtendWith(MockitoExtension.class)
class AsyncNoThrowTest {

    @Mock
    JsonSerializer serializer;

    @Mock
    @SuppressWarnings("unchecked")
    Transport<CompletableFuture<RawResponse>> transport;

    private AsyncArk client(boolean throwOnError) {
        return AsyncArkClient.builder()
                .serializer(serializer)
                .transport(transport)
                .baseUrl("https://api.example.com")
                .throwOnError(throwOnError)
                .build();
    }

    private AsyncArk defaultClient() {
        return AsyncArkClient.builder()
                .serializer(serializer)
                .transport(transport)
                .baseUrl("https://api.example.com")
                .build();
    }

    @Test
    void defaultBehavior_404_completesExceptionallyWithNotFoundException() {
        when(transport.send(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        new RawResponse(404, Map.of(), "{\"error\":\"not found\"}")));

        AsyncArk ark = defaultClient();
        CompletableFuture<ArkResponse<String>> future =
                ark.get("/users/1").retrieve().toEntity(String.class);

        ExecutionException ex = assertThrows(ExecutionException.class, future::get);
        assertInstanceOf(NotFoundException.class, ex.getCause());
    }

    @Test
    void perRequestNoThrow_404_returnsResponseWithStatus404() throws Exception {
        when(transport.send(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        new RawResponse(404, Map.of(), "{\"error\":\"not found\"}")));

        AsyncArk ark = defaultClient();
        ArkResponse<String> response =
                ark.get("/users/1").noThrow().retrieve().toEntity(String.class).get();

        assertEquals(404, response.statusCode());
        assertFalse(response.isSuccessful());
    }

    @Test
    void clientLevelThrowOnErrorFalse_404_isPermissive() throws Exception {
        when(transport.send(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        new RawResponse(404, Map.of(), "{\"error\":\"not found\"}")));

        AsyncArk ark = client(false);
        ArkResponse<String> response =
                ark.get("/users/1").retrieve().toEntity(String.class).get();

        assertEquals(404, response.statusCode());
        assertFalse(response.isSuccessful());
    }

    @Test
    void clientLevelThrowOnErrorFalse_withoutNoThrow_stillPermissive() throws Exception {
        when(transport.send(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        new RawResponse(500, Map.of(), "boom")));

        AsyncArk ark = client(false);
        ArkResponse<String> response =
                ark.get("/users/1").retrieve().toEntity(String.class).get();

        assertEquals(500, response.statusCode());
        assertFalse(response.isSuccessful());
    }
}
