# Quarkus Integration

Ark provides a Quarkus extension that auto-configures HTTP clients as CDI beans.

---

## Available Extensions

| Extension | Serializer | Dependency |
|-----------|------------|------------|
| `ark-quarkus-jackson` | Jackson 2.x (`JacksonClassicSerializer`) | `com.fasterxml.jackson` |

---

## Installation

```xml
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-quarkus-jackson</artifactId>
</dependency>
```

This auto-configures:
- `JsonSerializer` — using Quarkus-managed `ObjectMapper`
- `HttpTransport` — `ArkJdkHttpTransport` (sync)
- `MutinyHttpTransport` — `ArkVertxMutinyTransport` (reactive)
- `ArkClient.Builder` — sync, `@Dependent` scope
- `MutinyArkClient.Builder` — reactive, `@Dependent` scope

---

## Sync Usage

```java
@ApplicationScoped
public class UserService {

    private final Ark client;

    @Inject
    public UserService(ArkClient.Builder builder) {
        this.client = builder
            .baseUrl("https://api.example.com")
            .build();
    }

    public User getUser(String id) {
        return client.get("/users/" + id)
            .retrieve()
            .body(User.class);
    }
}
```

---

## Mutiny Usage

```java
@ApplicationScoped
public class ReactiveUserService {

    private final MutinyArk client;

    @Inject
    public ReactiveUserService(MutinyArkClient.Builder builder) {
        this.client = builder
            .baseUrl("https://api.example.com")
            .build();
    }

    public Uni<User> getUser(String id) {
        return client.get("/users/" + id)
            .retrieve()
            .body(User.class);
    }
}
```

---

## Quarkus REST Endpoint

```java
@Path("/users")
public class UserResource {

    @Inject MutinyArk client;

    @GET
    @Path("/{id}")
    public Uni<User> getUser(@PathParam("id") String id) {
        return client.get("/users/" + id)
            .retrieve()
            .body(User.class);
    }
}
```

---

## Native Image

Supported out of the box. No additional configuration needed.

---

## More Details

- [Quarkus Jackson Extension](quarkus-jackson.md) — full configuration, overrides, multiple clients
- [Mutiny Client](mutiny.md) — Mutiny-specific usage
- [Getting Started](getting-started.md)