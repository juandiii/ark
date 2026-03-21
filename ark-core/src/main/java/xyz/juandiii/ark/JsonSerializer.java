package xyz.juandiii.ark;

public interface JsonSerializer {
    String serialize(Object body);
    <T> T deserialize(String json, TypeRef<T> type);
}