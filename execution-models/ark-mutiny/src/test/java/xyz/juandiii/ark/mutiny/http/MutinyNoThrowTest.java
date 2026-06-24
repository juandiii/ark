package xyz.juandiii.ark.mutiny.http;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.core.exceptions.NotFoundException;
import xyz.juandiii.ark.core.http.ArkResponse;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.mutiny.MutinyArk;
import xyz.juandiii.ark.mutiny.MutinyArkClient;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Verifies the {@code noThrow()} per-request opt-out and the
 * client-level {@code throwOnError(false)} default for the Mutiny execution model.
 */
@ExtendWith(MockitoExtension.class)
class MutinyNoThrowTest {

    @Mock
    JsonSerializer serializer;

    @Mock
    MutinyHttpTransport transport;

    private MutinyArk client(boolean throwOnError) {
        return MutinyArkClient.builder()
                .serializer(serializer)
                .transport(transport)
                .baseUrl("https://api.example.com")
                .throwOnError(throwOnError)
                .build();
    }

    private MutinyArk defaultClient() {
        return MutinyArkClient.builder()
                .serializer(serializer)
                .transport(transport)
                .baseUrl("https://api.example.com")
                .build();
    }

    @Test
    void defaultBehavior_404_uniFailsWithNotFoundException() {
        when(transport.send(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(Uni.createFrom().item(new RawResponse(404, Map.of(), "{\"error\":\"not found\"}")));

        MutinyArk ark = defaultClient();
        UniAssertSubscriber<ArkResponse<String>> subscriber =
                ark.get("/users/1").retrieve().toEntity(String.class)
                        .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(NotFoundException.class);
    }

    @Test
    void perRequestNoThrow_404_returnsResponseWithStatus404() {
        when(transport.send(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(Uni.createFrom().item(new RawResponse(404, Map.of(), "{\"error\":\"not found\"}")));

        MutinyArk ark = defaultClient();
        ArkResponse<String> response = ark.get("/users/1").noThrow().retrieve()
                .toEntity(String.class)
                .await().indefinitely();

        assertEquals(404, response.statusCode());
        assertFalse(response.isSuccessful());
    }

    @Test
    void clientLevelThrowOnErrorFalse_404_isPermissive() {
        when(transport.send(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(Uni.createFrom().item(new RawResponse(404, Map.of(), "{\"error\":\"not found\"}")));

        MutinyArk ark = client(false);
        ArkResponse<String> response = ark.get("/users/1").retrieve()
                .toEntity(String.class)
                .await().indefinitely();

        assertEquals(404, response.statusCode());
        assertFalse(response.isSuccessful());
    }

    @Test
    void clientLevelThrowOnErrorFalse_withoutNoThrow_stillPermissive() {
        when(transport.send(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(Uni.createFrom().item(new RawResponse(500, Map.of(), "boom")));

        MutinyArk ark = client(false);
        ArkResponse<String> response = ark.get("/users/1").retrieve()
                .toEntity(String.class)
                .await().indefinitely();

        assertEquals(500, response.statusCode());
        assertFalse(response.isSuccessful());
    }
}
