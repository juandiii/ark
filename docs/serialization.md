# Serialization

Ark keeps serialization explicit and replaceable. The `JsonSerializer` interface is the only contract — swap implementations without changing client code.

---

## Built-in Serializers

| Serializer | Module | Library |
|------------|--------|---------|
| `JacksonSerializer` | `ark-jackson` | Jackson 3.x (tools.jackson) |
| `JacksonClassicSerializer` | `ark-jackson-classic` | Jackson 2.x (com.fasterxml) |
| `JsonbSerializer` | `ark-jsonb` | Jakarta JSON-B |

---

## Usage

### Manual

```java
JsonSerializer serializer = new JacksonSerializer(new ObjectMapper());

Ark client = ArkClient.builder()
    .serializer(serializer)
    .transport(transport)
    .baseUrl("https://api.example.com")
    .build();
```

### Spring Boot

Auto-configured — uses Spring's `ObjectMapper`:

```java
// JacksonSerializer is auto-registered as a bean
// Override with @Bean if needed:
@Bean
public JsonSerializer jsonSerializer(ObjectMapper objectMapper) {
    return new JacksonSerializer(objectMapper);
}
```

### Quarkus

Auto-configured — uses Quarkus-managed `ObjectMapper`:

```java
// JacksonClassicSerializer is auto-registered as a CDI bean
// Override with @Produces if needed:
@Produces
@Singleton
public JsonSerializer jsonSerializer(ObjectMapper objectMapper) {
    return new JacksonClassicSerializer(objectMapper);
}
```

---

## JsonSerializer Interface

```java
public interface JsonSerializer {

    String serialize(Object object);

    <T> T deserialize(String json, Class<T> type);

    <T> T deserialize(String json, TypeRef<T> typeRef);
}
```

All methods throw `ArkException` on serialization/deserialization failure.

---

## Generic Types

Use `TypeRef<T>` for generic types that lose type information at runtime:

```java
// Class<T> — simple types
User user = client.get("/users/1").retrieve().body(User.class);

// TypeRef<T> — generic types
List<User> users = client.get("/users").retrieve().body(new TypeRef<List<User>>() {});
Map<String, Object> data = client.get("/data").retrieve().body(new TypeRef<>() {});
```

---

## Custom Serializer

Implement `JsonSerializer` for any serialization library:

```java
public class GsonSerializer implements JsonSerializer {

    private final Gson gson;

    public GsonSerializer(Gson gson) {
        this.gson = gson;
    }

    @Override
    public String serialize(Object object) {
        return gson.toJson(object);
    }

    @Override
    public <T> T deserialize(String json, Class<T> type) {
        return gson.fromJson(json, type);
    }

    @Override
    public <T> T deserialize(String json, TypeRef<T> typeRef) {
        return gson.fromJson(json, typeRef.type());
    }
}
```

---

## Related

- [Getting Started](getting-started.md)
- [Design Principles](design.md)
- [Testing](testing.md)
