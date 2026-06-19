package xyz.juandiii.ark.async;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.juandiii.ark.async.http.AsyncClientRequest;
import xyz.juandiii.ark.async.http.AsyncClientResponse;
import xyz.juandiii.ark.core.http.ArkResponse;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsyncArkMockTest {

    @Mock
    AsyncArk asyncArk;

    @Mock
    AsyncClientRequest asyncRequest;

    @Mock
    AsyncClientResponse asyncResponse;

    @Nested
    class MockingInterfaces {

        @Test
        void givenMockedAsyncArk_whenGet_thenReturnsMockedRequest() {
            when(asyncArk.get("/users/1")).thenReturn(asyncRequest);
            when(asyncRequest.retrieve()).thenReturn(asyncResponse);
            when(asyncResponse.body(String.class))
                    .thenReturn(CompletableFuture.completedFuture("Juan"));

            CompletableFuture<String> result = asyncArk.get("/users/1")
                    .retrieve()
                    .body(String.class);

            assertEquals("Juan", result.join());
            verify(asyncArk).get("/users/1");
            verify(asyncRequest).retrieve();
            verify(asyncResponse).body(String.class);
        }

        @Test
        void givenMockedAsyncArk_whenPost_thenReturnsMockedResponse() {
            when(asyncArk.post("/users")).thenReturn(asyncRequest);
            when(asyncRequest.body(any())).thenReturn(asyncRequest);
            when(asyncRequest.retrieve()).thenReturn(asyncResponse);
            when(asyncResponse.toBodilessEntity())
                    .thenReturn(CompletableFuture.completedFuture(
                            new ArkResponse<>(201, Map.of(), null)));

            CompletableFuture<ArkResponse<Void>> result = asyncArk.post("/users")
                    .body("payload")
                    .retrieve()
                    .toBodilessEntity();

            assertEquals(201, result.join().statusCode());
        }

        @Test
        void givenMockedAsyncArk_whenGetWithToEntity_thenReturnsMockedArkResponse() {
            when(asyncArk.get("/users/1")).thenReturn(asyncRequest);
            when(asyncRequest.retrieve()).thenReturn(asyncResponse);
            when(asyncResponse.toEntity(String.class))
                    .thenReturn(CompletableFuture.completedFuture(
                            new ArkResponse<>(200, Map.of(), "Juan")));

            CompletableFuture<ArkResponse<String>> result = asyncArk.get("/users/1")
                    .retrieve()
                    .toEntity(String.class);

            ArkResponse<String> entity = result.join();
            assertEquals(200, entity.statusCode());
            assertEquals("Juan", entity.body());
        }
    }
}