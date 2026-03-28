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

## Logging

Enable request/response logging via the `LoggingInterceptor`:

```java
Ark client = ArkClient.builder()
    .serializer(serializer)
    .transport(transport)
    .baseUrl("https://api.example.com")
    .build();

LoggingInterceptor.apply(builder, LoggingInterceptor.Level.BODY);
```

| Level | Logs |
|-------|------|
| `OFF` | Nothing |
| `BASIC` | Method, URL, status, duration |
| `HEADERS` | BASIC + request/response headers |
| `BODY` | HEADERS + request/response body |

With Spring Boot or Quarkus, configure via `application.properties`:

```properties
ark.logging.level=BODY
```

---

## Error Handling

Ark provides typed exceptions for common HTTP errors:

| Exception | Status | Parent |
|-----------|--------|--------|
| `BadRequestException` | 400 | `ClientException` |
| `UnauthorizedException` | 401 | `ClientException` |
| `ForbiddenException` | 403 | `ClientException` |
| `NotFoundException` | 404 | `ClientException` |
| `ConflictException` | 409 | `ClientException` |
| `UnprocessableEntityException` | 422 | `ClientException` |
| `TooManyRequestsException` | 429 | `ClientException` |
| `InternalServerErrorException` | 500 | `ServerException` |
| `BadGatewayException` | 502 | `ServerException` |
| `ServiceUnavailableException` | 503 | `ServerException` |
| `GatewayTimeoutException` | 504 | `ServerException` |

Transport errors:

| Exception | When | Parent |
|-----------|------|--------|
| `TimeoutException` | Request timed out | `ArkException` |
| `ConnectionException` | Connection failed | `ArkException` |
| `RequestInterruptedException` | Thread interrupted | `ArkException` |

```java
try {
    User user = client.get("/users/1")
        .retrieve()
        .body(User.class);
} catch (NotFoundException e) {
    // 404
} catch (UnauthorizedException e) {
    // 401
} catch (ClientException e) {
    // other 4xx
} catch (ServerException e) {
    // 5xx
} catch (TimeoutException e) {
    // request timed out
} catch (ConnectionException e) {
    // connection failed
}
```

---

## Related

- [Spring Boot Integration](spring-boot.md)
- [Async Client](async.md)
- [Transport Model](transports.md)
- [Testing](testing.md)