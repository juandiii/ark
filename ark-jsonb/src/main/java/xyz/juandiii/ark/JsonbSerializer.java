package xyz.juandiii.ark;

import jakarta.json.bind.Jsonb;
import xyz.juandiii.ark.exceptions.ArkException;

import java.lang.reflect.Type;

/**
 * JSON serializer using Jakarta JSON-B (JSON Binding).
 * Use this for Quarkus with JSON-B, MicroProfile, or Jakarta EE projects.
 *
 * @author Juan Diego Lopez V.
 */
public class JsonbSerializer implements JsonSerializer {

    private final Jsonb jsonb;

    public JsonbSerializer(Jsonb jsonb) {
        this.jsonb = jsonb;
    }

    @Override
    public String serialize(Object body) {
        if (body == null) return null;
        if (body instanceof String s) return s;
        try {
            return jsonb.toJson(body);
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
            Type javaType = type.getType();
            return jsonb.fromJson(json, javaType);
        } catch (Exception e) {
            throw new ArkException("Failed to deserialize response: " + e.getMessage(), e);
        }
    }
}
