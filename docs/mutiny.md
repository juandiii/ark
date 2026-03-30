# Mutiny Client

The `MutinyArk` client returns `Uni<T>` from SmallRye Mutiny — ideal for Quarkus and reactive Vert.x applications.

---

## Installation

```xml
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-mutiny</artifactId>
</dependency>
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-transport-vertx-mutiny</artifactId>
</dependency>
```

Or use the Quarkus extension which auto-configures everything:

```xml
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-quarkus-jackson</artifactId>
</dependency>
```

---

## Building the Client

### Manual

```java
Vertx vertx = Vertx.vertx();
WebClient webClient = WebClient.create(vertx);

MutinyArk client = MutinyArkClient.builder()
    .serializer(new JacksonClassicSerializer(new ObjectMapper()))
    .transport(new ArkVertxMutinyTransport(webClient))
    .baseUrl("https://api.example.com")
    .build();
```

### With Quarkus Extension

```java
@ApplicationScoped
public class UserService {

    private final MutinyArk client;

    @Inject
    public UserService(MutinyArkClient.Builder builder) {
        this.client = builder
            .baseUrl("https://api.example.com")
            .build();
    }
}
```

---

## Making Requests

### GET

```java
Uni<User> user = client.get("/users/1")
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .body(User.class);
```

### GET with generics

```java
Uni<List<User>> users = client.get("/users")
    .queryParam("page", "1")
    .retrieve()
    .body(new TypeRef<List<User>>() {});
```

### POST

```java
Uni<User> created = client.post("/users")
    .contentType(MediaType.APPLICATION_JSON)
    .body(new User("Juan", "juan@example.com"))
    .retrieve()
    .body(User.class);
```

### DELETE

```java
Uni<ArkResponse<Void>> response = client.delete("/users/1")
    .retrieve()
    .toBodilessEntity();
```

### Full Response

```java
Uni<ArkResponse<User>> response = client.get("/users/1")
    .retrieve()
    .toEntity(User.class);

response.subscribe().with(r -> {
    int status = r.statusCode();
    User body = r.body();
    boolean ok = r.isSuccessful();
});
```

### Multi (streaming from JSON array)

```java
Multi<User> users = client.get("/users")
    .retrieve()
    .bodyAsMulti(User.class);
```

Deserializes the JSON array response and streams each element as a `Multi<T>`.

### Per-Request Timeout

```java
Uni<User> user = client.get("/slow-endpoint")
    .timeout(Duration.ofSeconds(60))
    .retrieve()
    .body(User.class);
```

---

## Usage in Quarkus REST

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

## Error Handling

See [Error Handling](error-handling.md) for the full exception hierarchy. Errors propagate through the `Uni`:

```java
client.get("/users/1")
    .retrieve()
    .body(User.class)
    .onFailure(NotFoundException.class).recoverWithItem(User.unknown())
    .onFailure(ServerException.class).retry().atMost(3);
```

---

## Related

- [Quarkus Jackson Extension](quarkus-jackson.md)
- [Vert.x Client](vertx.md)
- [Getting Started](getting-started.md)
- [Transport Model](transports.md)