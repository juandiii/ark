# Sync Client

The `Ark` client executes requests synchronously - blocking until the response is received.

---

## Installation

```xml
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-core</artifactId>
</dependency>
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-jackson</artifactId>
</dependency>
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-transport-jdk</artifactId>
</dependency>
```

Or use the Spring Boot starter:

```xml
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-spring-boot-starter</artifactId>
</dependency>
```

### Transport

- **Interface:** `HttpTransport extends Transport<RawResponse>` - returns `RawResponse` directly
- **Implementation:** `ArkJdkSyncTransport` - backed by Java's `HttpClient` (sync mode)

---

## Building the Client

### Manual

```java
Ark client = ArkClient.builder()
    .serializer(new JacksonSerializer(new ObjectMapper()))
    .transport(new ArkJdkSyncTransport(HttpClient.newBuilder().build()))
    .baseUrl("https://api.example.com")
    .build();
```

### With Spring Boot Starter

```java
@Configuration
public class HttpClientsConfig {

    @Bean
    public Ark apiClient(ArkClient.Builder builder) {
        return builder
            .baseUrl("https://api.example.com")
            .build();
    }
}
```

---

## Making Requests

### GET

```java
User user = client.get("/users/1")
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .body(User.class);
```

### GET with generics

```java
List<User> users = client.get("/users")
    .queryParam("page", "1")
    .queryParam("size", "20")
    .retrieve()
    .body(new TypeRef<List<User>>() {});
```

### POST

```java
User created = client.post("/users")
    .contentType(MediaType.APPLICATION_JSON)
    .body(new User("Juan", "juan@example.com"))
    .retrieve()
    .body(User.class);
```

### PUT

```java
User updated = client.put("/users/1")
    .contentType(MediaType.APPLICATION_JSON)
    .body(updatedUser)
    .retrieve()
    .body(User.class);
```

### DELETE

```java
client.delete("/users/1")
    .retrieve()
    .toBodilessEntity();
```

### Full Response

```java
ArkResponse<User> response = client.get("/users/1")
    .retrieve()
    .toEntity(User.class);

int status = response.statusCode();
User body = response.body();
boolean ok = response.isSuccessful();
```

### Per-Request Timeout

```java
User user = client.get("/slow-endpoint")
    .timeout(Duration.ofSeconds(60))
    .retrieve()
    .body(User.class);
```

### Raw String Response

```java
String html = client.get("/health")
    .retrieve()
    .body(String.class);
```

---

## Logging

See [Logging](logging.md) for full details. Quick setup:

```properties
ark.logging.level=BODY
```

Levels: `NONE`, `BASIC`, `HEADERS`, `BODY`.

---

## Error Handling

See [Error Handling](error-handling.md) for the full exception hierarchy and examples per execution model.

```java
try {
    User user = client.get("/users/1").retrieve().body(User.class);
} catch (NotFoundException e) {
    // 404
} catch (ServerException e) {
    // 5xx
} catch (TimeoutException e) {
    // request timed out
}
```

---

## Permissive error handling

By default, Ark throws an `ApiException` subtype for any HTTP 4xx/5xx
status. When 4xx is a meaningful business outcome (e.g. 404 = "not
found", not an error), opt out and inspect the response yourself.

Per-request opt-out via `.noThrow()`:

```java
ArkResponse<User> response = client.get("/users/1")
        .noThrow()
        .retrieve()
        .toEntity(User.class);

if (response.statusCode() == 404) return Optional.empty();
if (response.isSuccessful()) return Optional.of(response.body());
```

Client-level default via `throwOnError(false)`:

```java
Ark permissive = ArkClient.builder()
        .serializer(serializer)
        .transport(transport)
        .baseUrl("https://api.example.com")
        .throwOnError(false)
        .build();

// All requests on this client return responses regardless of status
ArkResponse<User> response = permissive.get("/users/1").retrieve().toEntity(User.class);
```

---

## Capturing the raw response

When you need the raw response — status, headers, and body as a String —
without going through deserialization (e.g. to inspect an error body that
doesn't match your typed schema), use `.raw()`:

```java
RawResponse raw = client.get("/users/1")
        .noThrow()
        .retrieve()
        .raw();

if (raw.isError()) {
    log.warn("Error {}: {}", raw.statusCode(), raw.body());
} else {
    User user = serializer.deserialize(raw.body(), User.class);
}
```

`.raw()` returns a `RawResponse` directly (no deserialization). Use it
together with `.noThrow()` (or client-level `throwOnError(false)`) to
inspect bodies on 4xx/5xx without exceptions.

---

## Related

- [Error Handling](error-handling.md) - full exception hierarchy
- [Spring Boot Integration](spring-boot.md)
- [Async Client](async.md)
- [Transport Model](transports.md)
- [Testing](testing.md)