package xyz.juandiii.ark.core;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.juandiii.ark.core.http.ArkResponse;
import xyz.juandiii.ark.core.http.ClientRequest;
import xyz.juandiii.ark.core.http.ClientResponse;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArkMockTest {

    @Mock
    Ark ark;

    @Mock
    ClientRequest clientRequest;

    @Mock
    ClientResponse clientResponse;

    @Nested
    class MockingInterfaces {

        @Test
        void givenMockedArk_whenGet_thenReturnsMockedRequest() {
            when(ark.get("/users/1")).thenReturn(clientRequest);
            when(clientRequest.retrieve()).thenReturn(clientResponse);
            when(clientResponse.body(String.class)).thenReturn("Juan");

            String result = ark.get("/users/1")
                    .retrieve()
                    .body(String.class);

            assertEquals("Juan", result);
            verify(ark).get("/users/1");
            verify(clientRequest).retrieve();
            verify(clientResponse).body(String.class);
        }

        @Test
        void givenMockedArk_whenPost_thenReturnsMockedResponse() {
            when(ark.post("/users")).thenReturn(clientRequest);
            when(clientRequest.body(any())).thenReturn(clientRequest);
            when(clientRequest.retrieve()).thenReturn(clientResponse);
            when(clientResponse.toBodilessEntity())
                    .thenReturn(new ArkResponse<>(201, Map.of(), null));

            ArkResponse<Void> response = ark.post("/users")
                    .body("payload")
                    .retrieve()
                    .toBodilessEntity();

            assertEquals(201, response.statusCode());
        }

        @Test
        void givenMockedArk_whenGetWithToEntity_thenReturnsMockedArkResponse() {
            when(ark.get("/users/1")).thenReturn(clientRequest);
            when(clientRequest.retrieve()).thenReturn(clientResponse);
            when(clientResponse.toEntity(String.class))
                    .thenReturn(new ArkResponse<>(200, Map.of(), "Juan"));

            ArkResponse<String> entity = ark.get("/users/1")
                    .retrieve()
                    .toEntity(String.class);

            assertEquals(200, entity.statusCode());
            assertEquals("Juan", entity.body());
            assertTrue(entity.isSuccessful());
        }
    }
}