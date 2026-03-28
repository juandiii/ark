# Async Client

The `AsyncArk` client returns `CompletableFuture<T>` — non-blocking execution using Java's built-in async primitives.

---

## Installation

```xml
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-async</artifactId>
</dependency>
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-transport-jdk</artifactId>
</dependency>
```

---

## Building the Client

```java
AsyncArk client = AsyncArkClient.builder()
    .serializer(new JacksonSerializer(new ObjectMapper()))
    .transport(new ArkJdkHttpTransport(HttpClient.newBuilder().build()))
    .baseUrl("https://api.example.com")
    .build();
```

## Transport

`ArkJdkHttpTransport` supports both sync and async execution by implementing both `HttpTransport` and `AsyncHttpTransport`.

```java
HttpClient httpClient = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_2)
    .connectTimeout(Duration.ofSeconds(10))
    .sslContext(sslContext)
    .executor(Executors.newVirtualThreadPerTaskExecutor())
    .build();

ArkJdkHttpTransport transport = new ArkJdkHttpTransport(httpClient);
```

---

## Making Requests

### GET

```java
CompletableFuture<User> user = client.get("/users/1")
    .retrieve()
    .body(User.class);
```

### POST

```java
CompletableFuture<User> created = client.post("/users")
    .contentType(MediaType.APPLICATION_JSON)
    .body(new User("Juan", "juan@example.com"))
    .retrieve()
    .body(User.class);
```

### Full Response

```java
CompletableFuture<ArkResponse<User>> response = client.get("/users/1")
    .retrieve()
    .toEntity(User.class);

response.thenAccept(r -> {
    int status = r.statusCode();
    User body = r.body();
});
```

### Per-Request Timeout

```java
CompletableFuture<User> user = client.get("/slow-endpoint")
    .timeout(Duration.ofSeconds(60))
    .retrieve()
    .body(User.class);
```

---

## Error Handling

Errors propagate through the `CompletableFuture`:

```java
client.get("/users/1")
    .retrieve()
    .body(User.class)
    .exceptionally(ex -> {
        if (ex.getCause() instanceof NotFoundException) {
            return User.unknown();
        }
        throw new CompletionException(ex);
    });
```

---

## Related

- [Sync Client](sync.md) — full error hierarchy reference
- [Getting Started](getting-started.md)
- [Transport Model](transports.md)