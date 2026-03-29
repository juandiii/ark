package xyz.juandiii.ark.mutiny.http;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.core.TypeRef;
import xyz.juandiii.ark.core.http.ArkResponse;
import xyz.juandiii.ark.core.http.RawResponse;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultMutinyClientResponseTest {

    @Mock
    JsonSerializer serializer;

    private DefaultMutinyClientResponse response(String body) {
        return new DefaultMutinyClientResponse(
                Uni.createFrom().item(new RawResponse(200, Map.of("Content-Type", List.of("application/json")), body)),
                serializer);
    }

    @Nested
    class Body {

        @Test
        void givenTypeRef_whenBody_thenDeserializesAsync() {
            when(serializer.deserialize(eq("{\"name\":\"Juan\"}"), any(TypeRef.class)))
                    .thenReturn("Juan");

            String result = response("{\"name\":\"Juan\"}")
                    .body(new TypeRef<String>() {})
                    .await().atMost(Duration.ofSeconds(5));

            assertEquals("Juan", result);
        }

        @Test
        void givenClass_whenBody_thenDeserializesAsync() {
            when(serializer.deserialize(eq("{\"name\":\"Juan\"}"), any(TypeRef.class)))
                    .thenReturn("Juan");

            String result = response("{\"name\":\"Juan\"}")
                    .body(String.class)
                    .await().atMost(Duration.ofSeconds(5));

            assertEquals("Juan", result);
        }
    }

    @Nested
    class ToEntity {

        @Test
        void givenTypeRef_whenToEntity_thenReturnsArkResponseAsync() {
            when(serializer.deserialize(eq("\"hello\""), any(TypeRef.class)))
                    .thenReturn("hello");

            ArkResponse<String> entity = response("\"hello\"")
                    .toEntity(new TypeRef<String>() {})
                    .await().atMost(Duration.ofSeconds(5));

            assertEquals(200, entity.statusCode());
            assertEquals("hello", entity.body());
            assertTrue(entity.isSuccessful());
        }

        @Test
        void givenClass_whenToEntity_thenReturnsArkResponseAsync() {
            when(serializer.deserialize(eq("\"hello\""), any(TypeRef.class)))
                    .thenReturn("hello");

            ArkResponse<String> entity = response("\"hello\"")
                    .toEntity(String.class)
                    .await().atMost(Duration.ofSeconds(5));

            assertEquals(200, entity.statusCode());
        }
    }

    @Nested
    class ToBodilessEntity {

        @Test
        void givenResponse_whenToBodilessEntity_thenReturnsVoidArkResponse() {
            ArkResponse<Void> entity = response("ignored")
                    .toBodilessEntity()
                    .await().atMost(Duration.ofSeconds(5));

            assertEquals(200, entity.statusCode());
            assertNull(entity.body());
            verifyNoInteractions(serializer);
        }
    }

    @Nested
    class ImplementsInterface {

        @Test
        void givenResponse_whenChecked_thenImplementsMutinyClientResponse() {
            assertInstanceOf(MutinyClientResponse.class, response("{}"));
        }
    }
}