package xyz.juandiii.ark;

/**
 * Strategy interface for JSON serialization and deserialization.
 *
 * @author Juan Diego Lopez V.
 */
public interface JsonSerializer {
    String serialize(Object body);
    <T> T deserialize(String json, TypeRef<T> type);
}