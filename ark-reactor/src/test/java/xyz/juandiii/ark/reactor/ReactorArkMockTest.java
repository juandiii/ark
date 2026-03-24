package xyz.juandiii.ark.reactor;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import xyz.juandiii.ark.http.ArkResponse;
import xyz.juandiii.ark.reactor.http.ReactorClientRequest;
import xyz.juandiii.ark.reactor.http.ReactorClientResponse;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactorArkMockTest {

    @Mock
    ReactorArk reactorArk;

    @Mock
    ReactorClientRequest reactorRequest;

    @Mock
    ReactorClientResponse reactorResponse;

    @Nested
    class MockingInterfaces {

        @Test
        void givenMockedReactorArk_whenGet_thenReturnsMockedRequest() {
            when(reactorArk.get("/users/1")).thenReturn(reactorRequest);
            when(reactorRequest.retrieve()).thenReturn(reactorResponse);
            when(reactorResponse.body(String.class))
                    .thenReturn(Mono.just("Juan"));

            Mono<String> result = reactorArk.get("/users/1")
                    .retrieve()
                    .body(String.class);

            assertEquals("Juan", result.block());
            verify(reactorArk).get("/users/1");
            verify(reactorRequest).retrieve();
            verify(reactorResponse).body(String.class);
        }

        @Test
        void givenMockedReactorArk_whenPost_thenReturnsMockedResponse() {
            when(reactorArk.post("/users")).thenReturn(reactorRequest);
            when(reactorRequest.body(any())).thenReturn(reactorRequest);
            when(reactorRequest.retrieve()).thenReturn(reactorResponse);
            when(reactorResponse.toBodilessEntity())
                    .thenReturn(Mono.just(new ArkResponse<>(201, Map.of(), null)));

            Mono<ArkResponse<Void>> result = reactorArk.post("/users")
                    .body("payload")
                    .retrieve()
                    .toBodilessEntity();

            assertEquals(201, result.block().statusCode());
        }

        @Test
        void givenMockedReactorArk_whenGetWithToEntity_thenReturnsMockedArkResponse() {
            when(reactorArk.get("/users/1")).thenReturn(reactorRequest);
            when(reactorRequest.retrieve()).thenReturn(reactorResponse);
            when(reactorResponse.toEntity(String.class))
                    .thenReturn(Mono.just(new ArkResponse<>(200, Map.of(), "Juan")));

            Mono<ArkResponse<String>> result = reactorArk.get("/users/1")
                    .retrieve()
                    .toEntity(String.class);

            ArkResponse<String> entity = result.block();
            assertEquals(200, entity.statusCode());
            assertEquals("Juan", entity.body());
        }
    }
}