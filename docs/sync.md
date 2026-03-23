# Sync Client

## Building the Client

```java
Ark client = ArkClient.builder()
    .serializer(new JacksonSerializer(new ObjectMapper()))
    .transport(new ArkJdkHttpTransport(HttpClient.newBuilder().build()))
    .baseUrl("https://api.example.com")
    .build();
```

## Making Requests

```java
User user = client.get("/users/1")
    .retrieve()
    .body(User.class);

String html = client.get("/health")
    .retrieve()
    .body(String.class);
```

### POST with JSON body

```java
User created = client.post("/users")
    .contentType(MediaType.APPLICATION_JSON)
    .body(new User("Juan", "juan@example.com"))
    .retrieve()
    .body(User.class);
```

### DELETE

```java
client.delete("/users/1")
    .retrieve()
    .toBodilessEntity();
```

### Full response extraction

```java
ArkResponse<User> response = client.get("/users/1")
    .retrieve()
    .toEntity(User.class);
```

---

## Error Handling

Ark distinguishes between HTTP-level errors and transport-level failures.

| Exception | When | Contains |
|---|---|---|
| `ApiException` | HTTP status `>= 400` | `statusCode()`, `responseBody()`, helpers like `isUnauthorized()` |
| `ArkException` | connection / IO / client failures | standard message and cause |

```java
try {
    User user = client.get("/users/1")
        .retrieve()
        .body(User.class);
} catch (ApiException e) {
    if (e.isNotFound()) {
        // 404
    } else if (e.isUnauthorized()) {
        // 401
    }
} catch (ArkException e) {
    log.error("Connection failed", e);
}
```