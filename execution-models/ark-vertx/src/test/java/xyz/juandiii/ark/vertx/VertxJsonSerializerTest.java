package xyz.juandiii.ark.vertx;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import xyz.juandiii.ark.core.TypeRef;
import xyz.juandiii.ark.core.exceptions.ArkException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VertxJsonSerializerTest {

    private VertxJsonSerializer serializer;

    @BeforeEach
    void setUp() {
        serializer = new VertxJsonSerializer();
    }

    @Nested
    class Serialize {

        @Test
        void givenNull_whenSerialize_thenReturnsNull() {
            assertNull(serializer.serialize(null));
        }

        @Test
        void givenString_whenSerialize_thenReturnsPassthrough() {
            assertEquals("hello", serializer.serialize("hello"));
        }

        @Test
        void givenJsonObject_whenSerialize_thenReturnsEncoded() {
            JsonObject json = new JsonObject().put("name", "Juan");
            String result = serializer.serialize(json);
            assertTrue(result.contains("\"name\""));
            assertTrue(result.contains("\"Juan\""));
        }

        @Test
        void givenJsonArray_whenSerialize_thenReturnsEncoded() {
            JsonArray arr = new JsonArray().add("a").add("b");
            String result = serializer.serialize(arr);
            assertTrue(result.contains("\"a\""));
            assertTrue(result.startsWith("["));
        }

        @Test
        void givenMap_whenSerialize_thenReturnsJson() {
            String result = serializer.serialize(Map.of("key", "value"));
            assertTrue(result.contains("\"key\""));
            assertTrue(result.contains("\"value\""));
        }

        @Test
        void givenList_whenSerialize_thenReturnsJsonArray() {
            String result = serializer.serialize(List.of("a", "b"));
            assertTrue(result.contains("\"a\""));
        }
    }

    @Nested
    class Deserialize {

        @Test
        void givenNull_whenDeserialize_thenReturnsNull() {
            assertNull(serializer.deserialize(null, new TypeRef<String>() {}));
        }

        @Test
        void givenBlank_whenDeserialize_thenReturnsNull() {
            assertNull(serializer.deserialize("", new TypeRef<String>() {}));
            assertNull(serializer.deserialize("   ", new TypeRef<String>() {}));
        }

        @Test
        void givenStringType_whenDeserialize_thenReturnsPassthrough() {
            assertEquals("raw text", serializer.deserialize("raw text", new TypeRef<String>() {}));
        }

        @Test
        void givenJsonObjectType_whenDeserialize_thenReturnsJsonObject() {
            JsonObject result = serializer.deserialize("{\"name\":\"Juan\"}", new TypeRef<JsonObject>() {});
            assertEquals("Juan", result.getString("name"));
        }

        @Test
        void givenJsonArrayType_whenDeserialize_thenReturnsJsonArray() {
            JsonArray result = serializer.deserialize("[\"a\",\"b\"]", new TypeRef<JsonArray>() {});
            assertEquals(2, result.size());
            assertEquals("a", result.getString(0));
        }

        @Test
        void givenMapClass_whenDeserialize_thenDecodesValue() {
            Map result = serializer.deserialize("{\"key\":\"value\"}", TypeRef.of(Map.class));
            assertEquals("value", result.get("key"));
        }

        @Test
        void givenInvalidJson_whenDeserialize_thenThrowsArkException() {
            assertThrows(ArkException.class, () ->
                    serializer.deserialize("not json", TypeRef.of(Map.class)));
        }

        @Test
        void givenArkException_whenDeserialize_thenContainsMessage() {
            ArkException ex = assertThrows(ArkException.class, () ->
                    serializer.deserialize("{invalid}", TypeRef.of(Map.class)));
            assertTrue(ex.getMessage().contains("Failed to deserialize"));
            assertNotNull(ex.getCause());
        }
    }
}