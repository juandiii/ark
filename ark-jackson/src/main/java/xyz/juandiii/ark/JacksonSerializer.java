package xyz.juandiii.ark;

import tools.jackson.databind.ObjectMapper;

public class JacksonSerializer implements JsonSerializer {

    private final ObjectMapper objectMapper;

    public JacksonSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String serialize(Object body) {
        if (body == null) return null;
        if (body instanceof String s) return s;
        return objectMapper.writeValueAsString(body);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(String json, TypeRef<T> type) {
        if (json == null || json.isBlank()) return null;
        if (type.getType() == String.class) return (T) json;
        return objectMapper.readValue(json, objectMapper.constructType(type.getType()));
    }
}
