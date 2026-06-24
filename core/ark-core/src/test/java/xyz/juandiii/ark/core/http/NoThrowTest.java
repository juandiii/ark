package xyz.juandiii.ark.core.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.juandiii.ark.core.Ark;
import xyz.juandiii.ark.core.ArkClient;
import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.core.exceptions.NotFoundException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Verifies the {@code noThrow()} per-request opt-out and the
 * client-level {@code throwOnError(false)} default for the sync execution model.
 */
@ExtendWith(MockitoExtension.class)
class NoThrowTest {

    @Mock
    JsonSerializer serializer;

    @Mock
    HttpTransport transport;

    private Ark client(boolean throwOnError) {
        return ArkClient.builder()
                .serializer(serializer)
                .transport(transport)
                .baseUrl("https://api.example.com")
                .throwOnError(throwOnError)
                .build();
    }

    private Ark defaultClient() {
        return ArkClient.builder()
                .serializer(serializer)
                .transport(transport)
                .baseUrl("https://api.example.com")
                .build();
    }

    @Test
    void defaultBehavior_404_throwsNotFoundException() {
        when(transport.send(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(new RawResponse(404, Map.of(), "{\"error\":\"not found\"}"));

        Ark ark = defaultClient();
        assertThrows(NotFoundException.class, () -> ark.get("/users/1").retrieve());
    }

    @Test
    void perRequestNoThrow_404_returnsResponseWithStatus404() {
        when(transport.send(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(new RawResponse(404, Map.of(), "{\"error\":\"not found\"}"));

        Ark ark = defaultClient();
        ArkResponse<String> response = ark.get("/users/1").noThrow().retrieve().toEntity(String.class);

        assertEquals(404, response.statusCode());
        assertFalse(response.isSuccessful());
    }

    @Test
    void clientLevelThrowOnErrorFalse_404_isPermissive() {
        when(transport.send(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(new RawResponse(404, Map.of(), "{\"error\":\"not found\"}"));

        Ark ark = client(false);
        ArkResponse<String> response = ark.get("/users/1").retrieve().toEntity(String.class);

        assertEquals(404, response.statusCode());
        assertFalse(response.isSuccessful());
    }

    @Test
    void clientLevelThrowOnErrorFalse_withoutNoThrow_stillPermissive() {
        when(transport.send(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(new RawResponse(500, Map.of(), "boom"));

        Ark ark = client(false);
        ArkResponse<String> response = ark.get("/users/1").retrieve().toEntity(String.class);

        assertEquals(500, response.statusCode());
        assertFalse(response.isSuccessful());
    }
}
