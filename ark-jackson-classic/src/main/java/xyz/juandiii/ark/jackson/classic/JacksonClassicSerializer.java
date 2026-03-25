package xyz.juandiii.ark.jackson.classic;

import com.fasterxml.jackson.databind.ObjectMapper;
import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.TypeRef;
import xyz.juandiii.ark.exceptions.ArkException;

/**
 * JSON serializer using Jackson 2.x (com.fasterxml) ObjectMapper.
 * Use this for Quarkus, Spring Boot 3.x, and legacy projects.
 *
 * @author Juan Diego Lopez V.
 */
public class JacksonClassicSerializer implements JsonSerializer {

    private final ObjectMapper objectMapper;

    public JacksonClassicSerializer(ObjectMapper objectMapper) {
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
