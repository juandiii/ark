# Getting Started

## Quick Start

### Sync

```java
Ark client = ArkClient.builder()
    .serializer(new JacksonSerializer(new ObjectMapper()))
    .transport(new ArkJdkHttpTransport(HttpClient.newBuilder().build()))
    .baseUrl("https://api.example.com")
    .build();

User user = client.get("/users/1")
    .retrieve()
    .body(User.class);
```

### Async

```java
AsyncArk client = AsyncArkClient.builder()
    .serializer(new JacksonSerializer(new ObjectMapper()))
    .transport(new ArkJdkHttpTransport(HttpClient.newBuilder().build()))
    .baseUrl("https://api.example.com")
    .build();

CompletableFuture<User> user = client.get("/users/1")
    .retrieve()
    .body(User.class);
```

### Reactor

```java
ReactorArk client = ReactorArkClient.builder()
    .serializer(new JacksonSerializer(new ObjectMapper()))
    .transport(new ArkReactorNettyTransport(httpClient))
    .baseUrl("https://api.example.com")
    .build();

Mono<User> user = client.get("/users/1")
    .retrieve()
    .body(User.class);
```

### Mutiny

```java
MutinyArk client = MutinyArkClient.builder()
    .serializer(serializer)
    .transport(new ArkVertxMutinyTransport(webClient))
    .baseUrl("https://api.example.com")
    .build();

Uni<User> user = client.get("/users/1")
    .retrieve()
    .body(User.class);
```

### Vert.x Future

```java
VertxArk client = VertxArkClient.builder()
    .serializer(serializer)
    .transport(new ArkVertxFutureTransport(webClient))
    .baseUrl("https://api.example.com")
    .build();

Future<User> user = client.get("/users/1")
    .retrieve()
    .body(User.class);
```

---

## Building a Client

All client variants share the same builder style.

```java
Ark client = ArkClient.builder()
    .serializer(serializer)
    .transport(new ArkJdkHttpTransport(httpClient))
    .baseUrl("https://api.example.com")
    .build();

AsyncArk asyncClient = AsyncArkClient.builder()
    .serializer(serializer)
    .transport(new ArkJdkHttpTransport(httpClient))
    .baseUrl("https://api.example.com")
    .build();

ReactorArk reactorClient = ReactorArkClient.builder()
    .serializer(serializer)
    .transport(new ArkReactorNettyTransport(reactorHttpClient))
    .baseUrl("https://api.example.com")
    .build();

MutinyArk mutinyClient = MutinyArkClient.builder()
    .serializer(serializer)
    .transport(new ArkVertxMutinyTransport(mutinyWebClient))
    .baseUrl("https://api.example.com")
    .build();

VertxArk vertxClient = VertxArkClient.builder()
    .serializer(serializer)
    .transport(new ArkVertxFutureTransport(webClient))
    .baseUrl("https://api.example.com")
    .build();
```

---

## Full Configuration Example

```java
HttpClient httpClient = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_2)
    .connectTimeout(Duration.ofSeconds(10))
    .followRedirects(HttpClient.Redirect.NORMAL)
    .sslContext(mySSLContext)
    .executor(Executors.newVirtualThreadPerTaskExecutor())
    .build();

Ark client = ArkClient.builder()
    .serializer(serializer)
    .transport(new ArkJdkHttpTransport(httpClient))
    .baseUrl("https://api.example.com")
    .userAgent("MyApp", "2.0")
    .requestInterceptor(request ->
        request.header("X-Request-Id", UUID.randomUUID().toString()))
    .responseInterceptor(response -> {
        log.info("Status: {}", response.statusCode());
        return response;
    })
    .build();
```

---

## Making Requests

### Simple response extraction

```java
User user = client.get("/users/1")
    .retrieve()
    .body(User.class);

String html = client.get("/health")
    .retrieve()
    .body(String.class);
```

### Generic response extraction with `TypeRef`

Use `TypeRef<T>` for generic types.

```java
List<User> users = client.get("/users")
    .retrieve()
    .body(new TypeRef<List<User>>() {});
```

### GET with query parameters

```java
List<User> users = client.get("/users")
    .queryParam("page", "1")
    .queryParam("size", "20")
    .retrieve()
    .body(new TypeRef<List<User>>() {});
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

### Per-request timeout

```java
User result = client.get("/slow-endpoint")
    .timeout(Duration.ofSeconds(120))
    .retrieve()
    .body(User.class);
```

---

## Same Fluent API, Different Return Types

Ark keeps the request flow consistent across all execution styles.

```java
User user = client.get("/users/1")
    .retrieve()
    .body(User.class);

CompletableFuture<User> cf = asyncClient.get("/users/1")
    .retrieve()
    .body(User.class);

Mono<User> mono = reactorClient.get("/users/1")
    .retrieve()
    .body(User.class);

Uni<User> uni = mutinyClient.get("/users/1")
    .retrieve()
    .body(User.class);

Future<User> future = vertxClient.get("/users/1")
    .retrieve()
    .body(User.class);
```
