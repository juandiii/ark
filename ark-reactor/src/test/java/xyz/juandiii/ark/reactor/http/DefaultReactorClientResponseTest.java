package xyz.juandiii.ark.reactor.http;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.TypeRef;
import xyz.juandiii.ark.http.ArkResponse;
import xyz.juandiii.ark.http.RawResponse;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultReactorClientResponseTest {

    @Mock
    JsonSerializer serializer;

    private DefaultReactorClientResponse response(String body) {
        return new DefaultReactorClientResponse(
                Mono.just(new RawResponse(200, Map.of("Content-Type", List.of("application/json")), body)),
                serializer);
    }

    @Nested
    class Body {

        @Test
        void givenTypeRef_whenBody_thenDeserializesReactive() {
            when(serializer.deserialize(eq("{\"name\":\"Juan\"}"), any(TypeRef.class)))
                    .thenReturn("Juan");

            Mono<String> mono = response("{\"name\":\"Juan\"}")
                    .body(new TypeRef<String>() {});

            StepVerifier.create(mono)
                    .expectNext("Juan")
                    .verifyComplete();
        }

        @Test
        void givenClass_whenBody_thenDeserializesReactive() {
            when(serializer.deserialize(eq("{\"name\":\"Juan\"}"), any(TypeRef.class)))
                    .thenReturn("Juan");

            Mono<String> mono = response("{\"name\":\"Juan\"}")
                    .body(String.class);

            StepVerifier.create(mono)
                    .expectNext("Juan")
                    .verifyComplete();
        }
    }

    @Nested
    class ToEntity {

        @Test
        void givenTypeRef_whenToEntity_thenReturnsArkResponseReactive() {
            when(serializer.deserialize(eq("\"hello\""), any(TypeRef.class)))
                    .thenReturn("hello");

            Mono<ArkResponse<String>> mono = response("\"hello\"")
                    .toEntity(new TypeRef<String>() {});

            StepVerifier.create(mono)
                    .assertNext(entity -> {
                        assertEquals(200, entity.statusCode());
                        assertEquals("hello", entity.body());
                        assertTrue(entity.isSuccessful());
                    })
                    .verifyComplete();
        }

        @Test
        void givenClass_whenToEntity_thenReturnsArkResponseReactive() {
            when(serializer.deserialize(eq("\"hello\""), any(TypeRef.class)))
                    .thenReturn("hello");

            Mono<ArkResponse<String>> mono = response("\"hello\"")
                    .toEntity(String.class);

            StepVerifier.create(mono)
                    .assertNext(entity -> assertEquals(200, entity.statusCode()))
                    .verifyComplete();
        }
    }

    @Nested
    class ToBodilessEntity {

        @Test
        void givenResponse_whenToBodilessEntity_thenReturnsVoidArkResponse() {
            Mono<ArkResponse<Void>> mono = response("ignored")
                    .toBodilessEntity();

            StepVerifier.create(mono)
                    .assertNext(entity -> {
                        assertEquals(200, entity.statusCode());
                        assertNull(entity.body());
                    })
                    .verifyComplete();

            verifyNoInteractions(serializer);
        }
    }

    @Nested
    class ImplementsInterface {

        @Test
        void givenResponse_whenChecked_thenImplementsReactorClientResponse() {
            assertInstanceOf(ReactorClientResponse.class, response("{}"));
        }
    }
}