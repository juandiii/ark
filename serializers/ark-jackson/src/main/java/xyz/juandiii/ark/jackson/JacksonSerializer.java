package xyz.juandiii.ark.jackson;

import tools.jackson.databind.ObjectMapper;
import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.core.TypeRef;
import xyz.juandiii.ark.core.exceptions.ArkException;

/**
 * JSON serializer implementation using Jackson ObjectMapper.
 *
 * @author Juan Diego Lopez V.
 */
public class JacksonSerializer implements JsonSerializer {

    private final ObjectMapper objectMapper;

    public JacksonSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String serialize(Object body) {
        if (body == null) return null;
        if (body instanceof String s) return s;
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new ArkException("Failed to serialize object: " + e.getMessage(), e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(String json, TypeRef<T> type) {
        if (json == null || json.isBlank()) return null;
        if (type.getType() == String.class) return (T) json;
        try {
            return objectMapper.readValue(json, objectMapper.constructType(type.getType()));
        } catch (Exception e) {
            throw new ArkException("Failed to deserialize response: " + e.getMessage(), e);
        }
    }
}
