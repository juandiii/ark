package xyz.juandiii.ark.reactor.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.core.exceptions.NotFoundException;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.reactor.ReactorArk;
import xyz.juandiii.ark.reactor.ReactorArkClient;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Verifies the {@code noThrow()} per-request opt-out and the
 * client-level {@code throwOnError(false)} default for the reactor execution model.
 */
@ExtendWith(MockitoExtension.class)
class ReactorNoThrowTest {

    @Mock
    JsonSerializer serializer;

    @Mock
    ReactorHttpTransport transport;

    private ReactorArk client(boolean throwOnError) {
        return ReactorArkClient.builder()
                .serializer(serializer)
                .transport(transport)
                .baseUrl("https://api.example.com")
                .throwOnError(throwOnError)
                .build();
    }

    private ReactorArk defaultClient() {
        return ReactorArkClient.builder()
                .serializer(serializer)
                .transport(transport)
                .baseUrl("https://api.example.com")
                .build();
    }

    @Test
    void defaultBehavior_404_monoEmitsNotFoundException() {
        when(transport.send(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(Mono.just(new RawResponse(404, Map.of(), "{\"error\":\"not found\"}")));

        ReactorArk ark = defaultClient();
        StepVerifier.create(ark.get("/users/1").retrieve().toEntity(String.class))
                .expectError(NotFoundException.class)
                .verify();
    }

    @Test
    void perRequestNoThrow_404_returnsResponseWithStatus404() {
        when(transport.send(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(Mono.just(new RawResponse(404, Map.of(), "{\"error\":\"not found\"}")));

        ReactorArk ark = defaultClient();
        StepVerifier.create(ark.get("/users/1").noThrow().retrieve().toEntity(String.class))
                .assertNext(response -> {
                    assertEquals(404, response.statusCode());
                    assertFalse(response.isSuccessful());
                })
                .verifyComplete();
    }

    @Test
    void clientLevelThrowOnErrorFalse_404_isPermissive() {
        when(transport.send(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(Mono.just(new RawResponse(404, Map.of(), "{\"error\":\"not found\"}")));

        ReactorArk ark = client(false);
        StepVerifier.create(ark.get("/users/1").retrieve().toEntity(String.class))
                .assertNext(response -> {
                    assertEquals(404, response.statusCode());
                    assertFalse(response.isSuccessful());
                })
                .verifyComplete();
    }

    @Test
    void clientLevelThrowOnErrorFalse_withoutNoThrow_stillPermissive() {
        when(transport.send(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(Mono.just(new RawResponse(500, Map.of(), "boom")));

        ReactorArk ark = client(false);
        StepVerifier.create(ark.get("/users/1").retrieve().toEntity(String.class))
                .assertNext(response -> {
                    assertEquals(500, response.statusCode());
                    assertFalse(response.isSuccessful());
                })
                .verifyComplete();
    }
}
