package xyz.juandiii.ark.vertx.http;

import io.vertx.core.Future;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultVertxClientResponseTest {

    @Mock
    JsonSerializer serializer;

    private DefaultVertxClientResponse response(String body) {
        return new DefaultVertxClientResponse(
                Future.succeededFuture(
                        new RawResponse(200, Map.of("Content-Type", List.of("application/json")), body)),
                serializer);
    }

    @Nested
    class Body {

        @Test
        void givenTypeRef_whenBody_thenDeserializesAsync() throws ExecutionException, InterruptedException, TimeoutException {
            when(serializer.deserialize(eq("{\"name\":\"Juan\"}"), any(TypeRef.class)))
                    .thenReturn("Juan");

            String result = response("{\"name\":\"Juan\"}")
                    .body(new TypeRef<String>() {})
                    .toCompletionStage().toCompletableFuture()
                    .get(5, TimeUnit.SECONDS);

            assertEquals("Juan", result);
        }

        @Test
        void givenClass_whenBody_thenDeserializesAsync() throws ExecutionException, InterruptedException, TimeoutException {
            when(serializer.deserialize(eq("{\"name\":\"Juan\"}"), any(TypeRef.class)))
                    .thenReturn("Juan");

            String result = response("{\"name\":\"Juan\"}")
                    .body(String.class)
                    .toCompletionStage().toCompletableFuture()
                    .get(5, TimeUnit.SECONDS);

            assertEquals("Juan", result);
        }
    }

    @Nested
    class ToEntity {

        @Test
        void givenTypeRef_whenToEntity_thenReturnsArkResponseAsync() throws ExecutionException, InterruptedException, TimeoutException {
            when(serializer.deserialize(eq("\"hello\""), any(TypeRef.class)))
                    .thenReturn("hello");

            ArkResponse<String> entity = response("\"hello\"")
                    .toEntity(new TypeRef<String>() {})
                    .toCompletionStage().toCompletableFuture()
                    .get(5, TimeUnit.SECONDS);

            assertEquals(200, entity.statusCode());
            assertEquals("hello", entity.body());
            assertTrue(entity.isSuccessful());
        }

        @Test
        void givenClass_whenToEntity_thenReturnsArkResponseAsync() throws ExecutionException, InterruptedException, TimeoutException {
            when(serializer.deserialize(eq("\"hello\""), any(TypeRef.class)))
                    .thenReturn("hello");

            ArkResponse<String> entity = response("\"hello\"")
                    .toEntity(String.class)
                    .toCompletionStage().toCompletableFuture()
                    .get(5, TimeUnit.SECONDS);

            assertEquals(200, entity.statusCode());
        }
    }

    @Nested
    class ToBodilessEntity {

        @Test
        void givenResponse_whenToBodilessEntity_thenReturnsVoidArkResponse() throws ExecutionException, InterruptedException, TimeoutException {
            ArkResponse<Void> entity = response("ignored")
                    .toBodilessEntity()
                    .toCompletionStage().toCompletableFuture()
                    .get(5, TimeUnit.SECONDS);

            assertEquals(200, entity.statusCode());
            assertNull(entity.body());
            verifyNoInteractions(serializer);
        }
    }

    @Nested
    class ImplementsInterface {

        @Test
        void givenResponse_whenChecked_thenImplementsVertxClientResponse() {
            assertInstanceOf(VertxClientResponse.class, response("{}"));
        }
    }
}