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
- `JsonSerializer` - using Quarkus-managed `ObjectMapper`
- `HttpTransport` - `ArkJdkHttpTransport` (sync)
- `MutinyHttpTransport` - `ArkVertxMutinyTransport` (reactive)
- `ArkClient.Builder` - sync, `@Dependent` scope
- `MutinyArkClient.Builder` - reactive, `@Dependent` scope

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

## Configuration

Configure globally and per-client via `application.properties`:

```properties
ark.logging.level=BODY
ark.client."user-api".base-url=https://api.example.com
ark.client."user-api".http-version=HTTP_2
ark.client."user-api".connect-timeout=5
ark.client."user-api".read-timeout=15
ark.client."user-api".tls-configuration-name=my-cert
```

See [Quarkus Jackson Extension](quarkus-jackson.md) for full configuration reference.

---

## Native Image

Supported out of the box. No additional configuration needed.

---

## More Details

- [Quarkus Jackson Extension](quarkus-jackson.md) - full configuration, overrides, TLS, logging
- [Declarative JAX-RS Clients](declarative-jaxrs.md) - `@RegisterArkClient` proxy clients
- [Mutiny Client](mutiny.md) - Mutiny-specific usage
- [Getting Started](getting-started.md)