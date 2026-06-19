package xyz.juandiii.ark.core;

/**
 * Strategy for JSON serialization and deserialization. Implementations bridge
 * Ark to a specific JSON library (e.g., Jackson). Implementations should be
 * stateless and thread-safe; Ark may call them concurrently across threads.
 *
 * @author Juan Diego Lopez V.
 */
public interface JsonSerializer {

    /**
     * Serialize a Java object to its JSON string representation.
     *
     * @param body object to serialize; may be {@code null} (implementations should return {@code null} or the JSON literal {@code "null"})
     * @return JSON string
     * @throws xyz.juandiii.ark.core.exceptions.ArkException if serialization fails
     */
    String serialize(Object body);

    /**
     * Deserialize a JSON string into a Java object of the given type.
     *
     * @param json JSON input
     * @param type runtime type token capturing generics
     * @param <T>  target Java type
     * @return deserialized instance
     * @throws xyz.juandiii.ark.core.exceptions.ArkException if the JSON is malformed or cannot be bound to {@code type}
     */
    <T> T deserialize(String json, TypeRef<T> type);
}
