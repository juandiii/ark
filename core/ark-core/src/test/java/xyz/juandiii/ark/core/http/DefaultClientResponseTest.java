package xyz.juandiii.ark.core.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.core.TypeRef;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultClientResponseTest {

    @Mock
    JsonSerializer serializer;

    private DefaultClientResponse response(String body) {
        return new DefaultClientResponse(
                new RawResponse(200, Map.of("Content-Type", List.of("application/json")), body),
                serializer);
    }

    @Test
    void bodyWithTypeRef() {
        when(serializer.deserialize(eq("{\"name\":\"Juan\"}"), any(TypeRef.class)))
                .thenReturn("Juan");

        Object result = response("{\"name\":\"Juan\"}").body(new TypeRef<String>() {});
        assertEquals("Juan", result);
        verify(serializer).deserialize(eq("{\"name\":\"Juan\"}"), any(TypeRef.class));
    }

    @Test
    void bodyWithClass() {
        when(serializer.deserialize(eq("{\"name\":\"Juan\"}"), any(TypeRef.class)))
                .thenReturn("Juan");

        Object result = response("{\"name\":\"Juan\"}").body(String.class);
        assertEquals("Juan", result);
    }

    @Test
    void toEntityWithTypeRef() {
        when(serializer.deserialize(eq("\"hello\""), any(TypeRef.class)))
                .thenReturn("hello");

        ArkResponse<String> entity = response("\"hello\"").toEntity(new TypeRef<String>() {});
        assertEquals(200, entity.statusCode());
        assertEquals("hello", entity.body());
        assertTrue(entity.isSuccessful());
        assertTrue(entity.headers().containsKey("Content-Type"));
    }

    @Test
    void toEntityWithClass() {
        when(serializer.deserialize(eq("\"hello\""), any(TypeRef.class)))
                .thenReturn("hello");

        ArkResponse<String> entity = response("\"hello\"").toEntity(String.class);
        assertEquals(200, entity.statusCode());
        assertEquals("hello", entity.body());
    }

    @Test
    void toBodilessEntity() {
        ArkResponse<Void> entity = response("ignored").toBodilessEntity();
        assertEquals(200, entity.statusCode());
        assertNull(entity.body());
        verifyNoInteractions(serializer);
    }
}