# Sync Client

The `Ark` client executes requests synchronously — blocking until the response is received.

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

- **Interface:** `HttpTransport` — returns `RawResponse`
- **Implementation:** `ArkJdkHttpTransport` — backed by Java's `HttpClient`

---

## Building the Client

### Manual

```java
Ark client = ArkClient.builder()
    .serializer(new JacksonSerializer(new ObjectMapper()))
    .transport(new ArkJdkHttpTransport(HttpClient.newBuilder().build()))
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

## Error Handling

| Exception | When | Contains |
|-----------|------|----------|
| `ApiException` | HTTP status >= 400 | `statusCode()`, `responseBody()`, `isUnauthorized()`, `isNotFound()` |
| `ArkException` | Connection/IO errors | `message`, `cause` |

```java
try {
    User user = client.get("/users/1")
        .retrieve()
        .body(User.class);
} catch (ApiException e) {
    if (e.isNotFound()) { /* 404 */ }
    else if (e.isUnauthorized()) { /* 401 */ }
} catch (ArkException e) {
    log.error("Connection failed", e);
}
```

---

## Related

- [Spring Boot Integration](spring-boot.md)
- [Async Client](async.md)
- [Transport Model](transports.md)
- [Testing](testing.md)