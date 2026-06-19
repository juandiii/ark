package xyz.juandiii.ark.vertx;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.core.TypeRef;
import xyz.juandiii.ark.core.exceptions.ArkException;

/**
 * JSON serializer using Vert.x built-in Jackson integration.
 *
 * @author Juan Diego Lopez V.
 */
@SuppressWarnings("unchecked")
public class VertxJsonSerializer implements JsonSerializer {

    @Override
    public String serialize(Object body) {
        if (body == null) return null;
        if (body instanceof String s) return s;
        if (body instanceof JsonObject json) return json.encode();
        if (body instanceof JsonArray arr) return arr.encode();
        try {
            return Json.encode(body);
        } catch (Exception e) {
            throw new ArkException("Failed to serialize object: " + e.getMessage(), e);
        }
    }

    @Override
    public <T> T deserialize(String json, TypeRef<T> type) {
        if (json == null || json.isBlank()) return null;
        if (type.getType() == String.class) return (T) json;
        if (type.getType() == JsonObject.class) return (T) new JsonObject(json);
        if (type.getType() == JsonArray.class) return (T) new JsonArray(json);
        try {
            return (T) Json.decodeValue(json, (Class<?>) type.getType());
        } catch (Exception e) {
            throw new ArkException("Failed to deserialize response: " + e.getMessage(), e);
        }
    }
}
