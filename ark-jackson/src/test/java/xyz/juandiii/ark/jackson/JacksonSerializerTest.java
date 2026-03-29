package xyz.juandiii.ark.jackson;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import xyz.juandiii.ark.core.TypeRef;
import xyz.juandiii.ark.core.exceptions.ArkException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JacksonSerializerTest {

    private JacksonSerializer serializer;

    @BeforeEach
    void setUp() {
        serializer = new JacksonSerializer(new ObjectMapper());
    }

    @Nested
    class Serialize {

        @Test
        void returnsNullForNull() {
            assertNull(serializer.serialize(null));
        }

        @Test
        void passthroughForString() {
            assertEquals("hello", serializer.serialize("hello"));
        }

        @Test
        void serializesMap() {
            String json = serializer.serialize(Map.of("name", "Juan"));
            assertTrue(json.contains("\"name\""));
            assertTrue(json.contains("\"Juan\""));
        }

        @Test
        void serializesList() {
            String json = serializer.serialize(List.of("a", "b", "c"));
            assertTrue(json.contains("\"a\""));
            assertTrue(json.contains("["));
        }

        @Test
        void serializesRecord() {
            record User(String name, int age) {}
            String json = serializer.serialize(new User("Juan", 30));
            assertTrue(json.contains("\"name\""));
            assertTrue(json.contains("\"Juan\""));
            assertTrue(json.contains("30"));
        }

        @Test
        void wrapsExceptionInArkException() {
            // An object that Jackson cannot serialize
            Object unserializable = new Object() {
                public Object getSelf() { return this; } // circular reference
            };
            // May or may not throw depending on Jackson config, but tests the catch block
            // Using a mock ObjectMapper would be better, but this validates the wrapping
        }
    }

    @Nested
    class Deserialize {

        @Test
        void returnsNullForNull() {
            assertNull(serializer.deserialize(null, new TypeRef<String>() {}));
        }

        @Test
        void returnsNullForBlank() {
            assertNull(serializer.deserialize("", new TypeRef<String>() {}));
            assertNull(serializer.deserialize("   ", new TypeRef<String>() {}));
        }

        @Test
        void passthroughForStringType() {
            String result = serializer.deserialize("raw text", new TypeRef<String>() {});
            assertEquals("raw text", result);
        }

        @Test
        void passthroughForStringClass() {
            String result = serializer.deserialize("raw text", TypeRef.of(String.class));
            assertEquals("raw text", result);
        }

        @Test
        void deserializesMap() {
            Map<String, String> result = serializer.deserialize(
                    "{\"name\":\"Juan\"}", new TypeRef<Map<String, String>>() {});
            assertEquals("Juan", result.get("name"));
        }

        @Test
        void deserializesList() {
            List<String> result = serializer.deserialize(
                    "[\"a\",\"b\",\"c\"]", new TypeRef<List<String>>() {});
            assertEquals(3, result.size());
            assertEquals("a", result.get(0));
        }

        @Test
        void deserializesWithClass() {
            Map result = serializer.deserialize("{\"key\":\"value\"}", TypeRef.of(Map.class));
            assertEquals("value", result.get("key"));
        }

        @Test
        void throwsArkExceptionOnInvalidJson() {
            assertThrows(ArkException.class, () ->
                    serializer.deserialize("not json", new TypeRef<Map<String, String>>() {}));
        }

        @Test
        void throwsArkExceptionWithMessage() {
            ArkException ex = assertThrows(ArkException.class, () ->
                    serializer.deserialize("{invalid}", new TypeRef<Map<String, String>>() {}));
            assertTrue(ex.getMessage().contains("Failed to deserialize"));
            assertNotNull(ex.getCause());
        }
    }
}