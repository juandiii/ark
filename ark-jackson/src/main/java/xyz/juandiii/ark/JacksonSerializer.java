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
    public <T> T deserialize(String json, TypeRef<T> type) {
        if (json == null || json.isBlank()) return null;
        return objectMapper.readValue(json, objectMapper.constructType(type.getType()));
    }
}
