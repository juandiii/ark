package xyz.juandiii.ark.async.http;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.TypeRef;
import xyz.juandiii.ark.http.ArkResponse;
import xyz.juandiii.ark.http.RawResponse;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultAsyncClientResponseTest {

    @Mock
    JsonSerializer serializer;

    private DefaultAsyncClientResponse response(String body) {
        return new DefaultAsyncClientResponse(
                CompletableFuture.completedFuture(
                        new RawResponse(200, Map.of("Content-Type", List.of("application/json")), body)),
                serializer);
    }

    @Nested
    class Body {

        @Test
        void givenTypeRef_whenBody_thenDeserializesAsync() throws Exception {
            when(serializer.deserialize(eq("{\"name\":\"Juan\"}"), any(TypeRef.class)))
                    .thenReturn("Juan");

            CompletableFuture<String> future = response("{\"name\":\"Juan\"}")
                    .body(new TypeRef<String>() {});

            assertEquals("Juan", future.get());
        }

        @Test
        void givenClass_whenBody_thenDeserializesAsync() throws Exception {
            when(serializer.deserialize(eq("{\"name\":\"Juan\"}"), any(TypeRef.class)))
                    .thenReturn("Juan");

            CompletableFuture<String> future = response("{\"name\":\"Juan\"}")
                    .body(String.class);

            assertEquals("Juan", future.get());
        }
    }

    @Nested
    class ToEntity {

        @Test
        void givenTypeRef_whenToEntity_thenReturnsArkResponseAsync() throws Exception {
            when(serializer.deserialize(eq("\"hello\""), any(TypeRef.class)))
                    .thenReturn("hello");

            CompletableFuture<ArkResponse<String>> future = response("\"hello\"")
                    .toEntity(new TypeRef<String>() {});

            ArkResponse<String> entity = future.get();
            assertEquals(200, entity.statusCode());
            assertEquals("hello", entity.body());
            assertTrue(entity.isSuccessful());
        }

        @Test
        void givenClass_whenToEntity_thenReturnsArkResponseAsync() throws Exception {
            when(serializer.deserialize(eq("\"hello\""), any(TypeRef.class)))
                    .thenReturn("hello");

            CompletableFuture<ArkResponse<String>> future = response("\"hello\"")
                    .toEntity(String.class);

            assertEquals(200, future.get().statusCode());
        }
    }

    @Nested
    class ToBodilessEntity {

        @Test
        void givenResponse_whenToBodilessEntity_thenReturnsVoidArkResponse() throws Exception {
            CompletableFuture<ArkResponse<Void>> future = response("ignored")
                    .toBodilessEntity();

            ArkResponse<Void> entity = future.get();
            assertEquals(200, entity.statusCode());
            assertNull(entity.body());
            verifyNoInteractions(serializer);
        }
    }

    @Nested
    class ImplementsInterface {

        @Test
        void givenResponse_whenChecked_thenImplementsAsyncClientResponse() {
            assertInstanceOf(AsyncClientResponse.class, response("{}"));
        }
    }
}
