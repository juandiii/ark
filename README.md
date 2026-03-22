# Ark

A lightweight, fluent HTTP client library for Java 25+ with pluggable transport and serialization. Ark provides a clean three-phase API: **method → configure → retrieve → extract**, with fully separated sync, async, reactive, and Mutiny interfaces.

## Quick Start

```java
// Sync
Ark client = ArkClient.builder()
    .serializer(new JacksonSerializer(new ObjectMapper()))
    .transport(new ArkJdkHttpTransport(HttpClient.newBuilder().build()))
    .baseUrl("https://api.example.com")
    .build();

User user = client.get("/users/1")
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .body(User.class);
```

### Maven

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>xyz.juandiii</groupId>
            <artifactId>ark-bom</artifactId>
            <version>1.0-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Pick the modules you need:

```xml
<!-- Core + Jackson + JDK transport (sync + async) -->
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

<!-- Add async support -->
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-async</artifactId>
</dependency>

<!-- Or Reactor support -->
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-reactor</artifactId>
</dependency>
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-transport-reactor</artifactId>
</dependency>

<!-- Or Mutiny support (Quarkus) -->
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-mutiny</artifactId>
</dependency>
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-transport-vertx-mutiny</artifactId>
</dependency>
```

For Spring Boot projects:

```xml
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-spring-boot-starter</artifactId>
</dependency>
```

---

## Building the Client

Ark provides separate entry points for each execution model. Each uses the same fluent configuration via `AbstractBuilder`.

```java
Ark client          = ArkClient.builder()        .serializer(s).transport(new ArkJdkHttpTransport(httpClient)).baseUrl(url).build();
AsyncArk async      = AsyncArkClient.builder()   .serializer(s).transport(new ArkJdkHttpTransport(httpClient)).baseUrl(url).build();
ReactorArk reactor  = ReactorArkClient.builder()  .serializer(s).transport(new ArkReactorNettyTransport(netty)).baseUrl(url).build();
MutinyArk mutiny    = MutinyArkClient.builder()   .serializer(s).transport(new ArkVertxMutinyTransport(wc)).baseUrl(url).build();
VertxArk vertx      = VertxArkClient.builder()    .serializer(s).transport(new ArkVertxFutureTransport(wc)).baseUrl(url).build();
```

### Full Configuration

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

All execution models use the same fluent API. Only the return types differ.

Response extraction supports both `Class<T>` (simple types) and `TypeRef<T>` (generics):

```java
// Class<T> — simple, for non-generic types
User user = client.get("/users/1").retrieve().body(User.class);
String html = client.get("/health").retrieve().body(String.class);

// TypeRef<T> — required for generic types
List<User> users = client.get("/users").retrieve().body(new TypeRef<List<User>>() {});
```

```java
// GET
User user = client.get("/users/1").retrieve().body(User.class);

// GET with query params and generics
List<User> users = client.get("/users")
    .queryParam("page", "1").queryParam("size", "20")
    .retrieve()
    .body(new TypeRef<List<User>>() {});

// POST
User created = client.post("/users")
    .contentType(MediaType.APPLICATION_JSON)
    .body(new User("Juan", "juan@example.com"))
    .retrieve()
    .body(User.class);

// DELETE
client.delete("/users/1").retrieve().toBodilessEntity();

// Full response (status + headers + body)
ArkResponse<User> response = client.get("/users/1").retrieve().toEntity(User.class);

// Per-request timeout
User slow = client.get("/slow-endpoint").timeout(Duration.ofSeconds(120)).retrieve().body(User.class);
```

Same API across all execution models — only the return type changes:

```java
User user                    = client.get("/users/1").retrieve().body(User.class);         // sync
CompletableFuture<User> cf   = asyncClient.get("/users/1").retrieve().body(User.class);    // async
Mono<User> mono              = reactorClient.get("/users/1").retrieve().body(User.class);  // reactor
Uni<User> uni                = mutinyClient.get("/users/1").retrieve().body(User.class);   // mutiny
Future<User> future          = vertxClient.get("/users/1").retrieve().body(User.class);    // vertx
```

---

## HTTP Transport

Ark uses a **bridge pattern** — the transport is a thin adapter that wraps an already-configured HTTP client. All settings (timeouts, SSL, connection pools, HTTP version) are configured on the client itself.

Five transport interfaces, one per execution model:

| Interface | Returns | Module |
|-----------|---------|--------|
| `HttpTransport` | `RawResponse` | ark-core |
| `AsyncHttpTransport` | `CompletableFuture<RawResponse>` | ark-async |
| `ReactorHttpTransport` | `Mono<RawResponse>` | ark-reactor |
| `MutinyHttpTransport` | `Uni<RawResponse>` | ark-mutiny |
| `VertxHttpTransport` | `Future<RawResponse>` | ark-vertx |

A transport can implement multiple interfaces. `ArkJdkHttpTransport` implements both `HttpTransport` and `AsyncHttpTransport`.

### Java Native (ArkJdkHttpTransport)

```java
HttpClient httpClient = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_2)
    .connectTimeout(Duration.ofSeconds(10))
    .sslContext(sslContext)
    .executor(Executors.newVirtualThreadPerTaskExecutor())
    .build();

// Implements HttpTransport + AsyncHttpTransport
ArkJdkHttpTransport transport = new ArkJdkHttpTransport(httpClient);
ArkJdkHttpTransport transport = new ArkJdkHttpTransport(httpClient, customExecutor);
```

### Reactor Netty (ArkReactorNettyTransport)

```java
reactor.netty.http.client.HttpClient httpClient = HttpClient.create()
    .responseTimeout(Duration.ofSeconds(30))
    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
    .secure(ssl -> ssl.sslContext(sslContext));

// Implements ReactorHttpTransport
ArkReactorNettyTransport transport = new ArkReactorNettyTransport(httpClient);
```

### Vert.x (ArkVertxTransport — CompletableFuture)

```java
WebClient webClient = WebClient.create(vertx, new WebClientOptions()
    .setSsl(true)
    .setConnectTimeout(5000)
    .setMaxPoolSize(50));

// CompletableFuture — implements AsyncHttpTransport
ArkVertxTransport transport = new ArkVertxTransport(webClient);
```

### Vert.x (ArkVertxFutureTransport — io.vertx.core.Future)

```java
// Vert.x Future nativo — implements VertxHttpTransport
ArkVertxFutureTransport transport = new ArkVertxFutureTransport(webClient);
```

### Vert.x Mutiny (ArkVertxMutinyTransport)

```java
io.vertx.mutiny.ext.web.client.WebClient webClient = WebClient.create(vertx);

// Uni nativo — implements MutinyHttpTransport
ArkVertxMutinyTransport transport = new ArkVertxMutinyTransport(webClient);
```

### Custom Transport

```java
public class MyTransport implements HttpTransport {

    private final MyHttpClient client;

    public MyTransport(MyHttpClient client) {
        this.client = client;
    }

    @Override
    public RawResponse send(String method, URI uri, Map<String, String> headers,
                            String body, Duration timeout) {
        // Adapt to your client's API and return RawResponse
    }
}
```

---

## Interceptors

### Request Interceptor

Runs before the HTTP call. Receives `RequestContext` — works with all execution models.

```java
.requestInterceptor(request -> {
    request.header("Authorization", "Bearer " + tokenService.getToken());
    request.header("X-Request-Id", UUID.randomUUID().toString());
})
```

The lambda executes on every request — `tokenService.getToken()` is called each time, ensuring fresh tokens.

### Response Interceptor

Runs after the HTTP call. Can inspect or transform the response.

```java
.responseInterceptor(response -> {
    log.info("HTTP {} - {} bytes", response.statusCode(), response.body().length());
    return response;
})
```

Multiple interceptors execute in registration order. In reactive modes, they are chained via `map` (Reactor), `onItem().transform` (Mutiny), or `thenApply` (async).

---

## Error Handling

| Exception | When | Contains |
|-----------|------|----------|
| `ApiException` | HTTP status >= 400 | `statusCode()`, `responseBody()`, `isUnauthorized()`, `isNotFound()` |
| `ArkException` | Connection/IO errors | Standard `message` and `cause` |

```java
try {
    User user = client.get("/users/1")
        .retrieve()
        .body(new TypeRef<User>() {});
} catch (ApiException e) {
    if (e.isNotFound()) { /* 404 */ }
    else if (e.isUnauthorized()) { /* 401 */ }
} catch (ArkException e) {
    log.error("Connection failed", e);
}
```

---

## Spring Boot Integration

The `ark-spring-boot-starter` auto-configures:
1. A `JsonSerializer` bean (JacksonSerializer)
2. A prototype-scoped `ArkClient.Builder` pre-configured with the serializer

```java
@Configuration
public class HttpClientsConfig {

    @Bean
    public Ark oauthClient(ArkClient.Builder arkBuilder) {
        return arkBuilder
            .transport(new ArkJdkHttpTransport(HttpClient.newBuilder().build()))
            .baseUrl("https://oauth.provider.com")
            .build();
    }

    @Bean
    public Ark apiClient(ArkClient.Builder arkBuilder) {
        return arkBuilder
            .transport(new ArkJdkHttpTransport(HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .connectTimeout(Duration.ofSeconds(10))
                    .build()))
            .baseUrl("https://api.myservice.com")
            .requestInterceptor(req ->
                req.header("Authorization", "Bearer " + tokenService.getToken()))
            .build();
    }
}
```

## Quarkus Integration

```java
@ApplicationScoped
public class HttpConfig {

    @Produces
    public MutinyArk apiClient(Vertx vertx, JsonSerializer serializer) {
        return MutinyArkClient.builder()
            .serializer(serializer)
            .transport(new ArkVertxMutinyTransport(WebClient.create(vertx)))
            .baseUrl("https://api.example.com")
            .build();
    }
}
```

---

## Custom Serializer

```java
public class GsonSerializer implements JsonSerializer {

    private final Gson gson = new Gson();

    @Override
    public String serialize(Object body) {
        if (body == null) return null;
        if (body instanceof String s) return s;
        return gson.toJson(body);
    }

    @Override
    public <T> T deserialize(String json, TypeRef<T> type) {
        return gson.fromJson(json, type.getType());
    }
}
```

---

## Requirements

- Java 11+
- Jackson (for `ark-jackson`)
- Spring Boot 4.0+ (for `ark-spring-boot-starter`)
- Reactor Core (for `ark-reactor`)
- Reactor Netty (for `ark-transport-reactor`)
- Vert.x Core (for `ark-vertx`)
- Vert.x Web Client (for `ark-transport-vertx`)
- SmallRye Mutiny + Vert.x Mutiny (for `ark-mutiny` + `ark-transport-vertx-mutiny`)

## Building

```bash
mvn clean install              # Full build with tests
mvn clean install -DskipTests  # Full build without tests
mvn test                       # Run tests only
```

## License

MIT
