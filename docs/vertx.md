# Vert.x Client

The `VertxArk` client returns `io.vertx.core.Future<T>` — Vert.x native async without Mutiny or CompletableFuture.

---

## Installation

```xml
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-vertx</artifactId>
</dependency>
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-transport-vertx</artifactId>
</dependency>
```

### Transport

- **Interface:** `VertxHttpTransport` — returns `Future<RawResponse>`
- **Implementation:** `ArkVertxFutureTransport` — backed by Vert.x `WebClient`

---

## Building the Client

```java
Vertx vertx = Vertx.vertx();
WebClient webClient = WebClient.create(vertx, new WebClientOptions()
    .setSsl(true)
    .setConnectTimeout(5000)
    .setMaxPoolSize(50));

VertxArk client = VertxArkClient.builder()
    .serializer(new JacksonClassicSerializer(new ObjectMapper()))
    .transport(new ArkVertxFutureTransport(webClient))
    .baseUrl("https://api.example.com")
    .build();
```

---

## Making Requests

### GET

```java
Future<User> user = client.get("/users/1")
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .body(User.class);
```

### POST

```java
Future<User> created = client.post("/users")
    .contentType(MediaType.APPLICATION_JSON)
    .body(new User("Juan", "juan@example.com"))
    .retrieve()
    .body(User.class);
```

### DELETE

```java
Future<ArkResponse<Void>> response = client.delete("/users/1")
    .retrieve()
    .toBodilessEntity();
```

### Full Response

```java
Future<ArkResponse<User>> response = client.get("/users/1")
    .retrieve()
    .toEntity(User.class);

response.onSuccess(r -> {
    int status = r.statusCode();
    User body = r.body();
});
```

---

## Vert.x Composition

Use Vert.x `Future` API for chaining:

```java
client.get("/users/1")
    .retrieve()
    .body(User.class)
    .compose(user -> saveToDb(user))
    .onSuccess(saved -> log.info("Saved: {}", saved))
    .onFailure(err -> log.error("Failed", err));
```

---

## Error Handling

Errors propagate through the `Future`. Use typed exceptions:

```java
client.get("/users/1")
    .retrieve()
    .body(User.class)
    .onFailure(err -> {
        if (err instanceof NotFoundException) {
            // 404
        } else if (err instanceof TimeoutException) {
            // request timed out
        }
    });
```

---

## Related

- [Mutiny Client](mutiny.md)
- [Transport Model](transports.md)
- [Getting Started](getting-started.md)