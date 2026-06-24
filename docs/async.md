# Async Client

The `AsyncArk` client returns `CompletableFuture<T>` - non-blocking execution using Java's built-in async primitives.

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
    .transport(new ArkJdkAsyncTransport(HttpClient.newBuilder().build()))
    .baseUrl("https://api.example.com")
    .build();
```

## Transport

`ArkJdkAsyncTransport` implements `Transport<CompletableFuture<RawResponse>>`. It is a sibling of `ArkJdkSyncTransport`: both can wrap the same underlying Java `HttpClient` if you want to share the connection pool between sync and async modes.

```java
HttpClient httpClient = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_2)
    .connectTimeout(Duration.ofSeconds(10))
    .sslContext(sslContext)
    .executor(Executors.newVirtualThreadPerTaskExecutor())
    .build();

ArkJdkAsyncTransport asyncTransport = new ArkJdkAsyncTransport(httpClient);
// ArkJdkSyncTransport syncTransport = new ArkJdkSyncTransport(httpClient);  // shares pool
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

See [Error Handling](error-handling.md) for the full exception hierarchy. Errors propagate through the `CompletableFuture`:

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

## Permissive error handling

By default, Ark fails the `CompletableFuture` with an `ApiException`
subtype for any HTTP 4xx/5xx status. When 4xx is a meaningful business
outcome, opt out and inspect the response.

Per-request opt-out via `.noThrow()`:

```java
CompletableFuture<ArkResponse<User>> response = client.get("/users/1")
        .noThrow()
        .retrieve()
        .toEntity(User.class);

response.thenAccept(r -> {
    if (r.statusCode() == 404) {
        // not found, treat as business outcome
    } else if (r.isSuccessful()) {
        User body = r.body();
    }
});
```

Client-level default via `throwOnError(false)`:

```java
AsyncArk permissive = AsyncArkClient.builder()
        .serializer(serializer)
        .transport(transport)
        .baseUrl("https://api.example.com")
        .throwOnError(false)
        .build();
```

---

## Capturing the raw response

When you need the raw response — status, headers, and body as a String —
without going through deserialization (e.g. to inspect an error body that
doesn't match your typed schema), use `.raw()`:

```java
CompletableFuture<RawResponse> futureRaw = client.get("/users/1")
        .noThrow()
        .retrieve()
        .raw();

futureRaw.thenAccept(raw -> {
    if (raw.isError()) {
        log.warn("Error {}: {}", raw.statusCode(), raw.body());
    } else {
        User user = serializer.deserialize(raw.body(), User.class);
    }
});
```

`.raw()` returns a `CompletableFuture<RawResponse>` (no deserialization).
Use it together with `.noThrow()` (or client-level `throwOnError(false)`)
to inspect bodies on 4xx/5xx without the future failing.

---

## Related

- [Error Handling](error-handling.md)
- [Getting Started](getting-started.md)
- [Transport Model](transports.md)