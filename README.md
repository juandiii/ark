# Ark

A lightweight, fluent HTTP client library for Java 25+ with pluggable transport and serialization. Ark provides a clean three-phase API: **method → configure → retrieve → extract**, with fully separated sync, async, reactive, and Mutiny interfaces.

## Quick Start

```java
// Sync
Ark client = ArkClient.sync()
    .serializer(new JacksonSerializer(new ObjectMapper()))
    .transport(new NativeHttpTransport(HttpClient.newBuilder().build()))
    .baseUrl("https://api.example.com")
    .build();

User user = client.get("/users/1")
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .body(new TypeRef<User>() {});
```

## Modules

```
ark-core                       Core interfaces and fluent API — zero external dependencies
ark-async                      Async support with CompletableFuture
ark-reactor                    Reactive support with Mono/Flux
ark-mutiny                     Mutiny support with Uni
ark-jackson                    Jackson-based JsonSerializer implementation
ark-transport-jdk              NativeHttpTransport — java.net.http.HttpClient (sync + async)
ark-transport-reactor          ReactorNettyTransport — Reactor Netty
ark-transport-vertx            VertxHttpTransport — Vert.x WebClient (CompletableFuture)
ark-transport-vertx-mutiny     VertxMutinyTransport — Vert.x Mutiny WebClient (Uni)
ark-spring-boot-starter        Spring Boot auto-configuration
ark-bom                        Bill of Materials for version management
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

### Sync

```java
Ark client = ArkClient.sync()
    .serializer(new JacksonSerializer(new ObjectMapper()))
    .transport(new NativeHttpTransport(HttpClient.newBuilder().build()))
    .baseUrl("https://api.example.com")
    .build();
```

### Async (CompletableFuture)

```java
AsyncArk asyncClient = AsyncArkClient.builder()
    .serializer(serializer)
    .transport(new NativeHttpTransport(HttpClient.newBuilder().build()))
    .baseUrl("https://api.example.com")
    .build();
```

### Reactor (Mono/Flux)

```java
ReactorArk reactorClient = ReactorArkClient.builder()
    .serializer(serializer)
    .transport(new ReactorNettyTransport(HttpClient.create()))
    .baseUrl("https://api.example.com")
    .build();
```

### Mutiny (Uni)

```java
MutinyArk mutinyClient = MutinyArkClient.builder()
    .serializer(serializer)
    .transport(new VertxMutinyTransport(WebClient.create(vertx)))
    .baseUrl("https://api.example.com")
    .build();
```

### Full Configuration

All builders share the same configuration methods:

```java
HttpClient httpClient = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_2)
    .connectTimeout(Duration.ofSeconds(10))
    .followRedirects(HttpClient.Redirect.NORMAL)
    .sslContext(mySSLContext)
    .executor(Executors.newVirtualThreadPerTaskExecutor())
    .build();

Ark client = ArkClient.sync()
    .serializer(serializer)
    .transport(new NativeHttpTransport(httpClient))
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

### Sync

```java
User user = client.get("/users/1")
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .body(new TypeRef<User>() {});

List<User> users = client.get("/users")
    .queryParam("page", "1")
    .queryParam("size", "20")
    .retrieve()
    .body(new TypeRef<List<User>>() {});

User created = client.post("/users")
    .contentType(MediaType.APPLICATION_JSON)
    .body(new User("Juan", "juan@example.com"))
    .retrieve()
    .body(new TypeRef<User>() {});

client.delete("/users/1")
    .retrieve()
    .toBodilessEntity();
```

### Async

```java
CompletableFuture<User> future = asyncClient.get("/users/1")
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .body(new TypeRef<User>() {});
```

### Reactor

```java
Mono<User> mono = reactorClient.get("/users/1")
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .body(new TypeRef<User>() {});
```

### Mutiny

```java
Uni<User> uni = mutinyClient.get("/users/1")
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .body(new TypeRef<User>() {});
```

### Full Response (status + headers + body)

```java
ArkResponse<User> response = client.get("/users/1")
    .retrieve()
    .toEntity(new TypeRef<User>() {});

int status = response.statusCode();
Map<String, List<String>> headers = response.headers();
User body = response.body();
boolean ok = response.isSuccessful(); // 2xx
```

### Per-Request Timeout

```java
User user = client.get("/slow-endpoint")
    .timeout(Duration.ofSeconds(120))
    .retrieve()
    .body(new TypeRef<User>() {});
```

---

## HTTP Transport

Ark uses a **bridge pattern** — the transport is a thin adapter that wraps an already-configured HTTP client. All settings (timeouts, SSL, connection pools, HTTP version) are configured on the client itself.

Four transport interfaces, one per execution model:

| Interface | Returns | Module |
|-----------|---------|--------|
| `HttpTransport` | `RawResponse` | ark-core |
| `AsyncHttpTransport` | `CompletableFuture<RawResponse>` | ark-async |
| `ReactorHttpTransport` | `Mono<RawResponse>` | ark-reactor |
| `MutinyHttpTransport` | `Uni<RawResponse>` | ark-mutiny |

A transport can implement multiple interfaces. `NativeHttpTransport` implements both `HttpTransport` and `AsyncHttpTransport`.

### Java Native (NativeHttpTransport)

```java
HttpClient httpClient = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_2)
    .connectTimeout(Duration.ofSeconds(10))
    .sslContext(sslContext)
    .executor(Executors.newVirtualThreadPerTaskExecutor())
    .build();

// Implements HttpTransport + AsyncHttpTransport
NativeHttpTransport transport = new NativeHttpTransport(httpClient);
NativeHttpTransport transport = new NativeHttpTransport(httpClient, customExecutor);
```

### Reactor Netty (ReactorNettyTransport)

```java
reactor.netty.http.client.HttpClient httpClient = HttpClient.create()
    .responseTimeout(Duration.ofSeconds(30))
    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
    .secure(ssl -> ssl.sslContext(sslContext));

// Implements ReactorHttpTransport
ReactorNettyTransport transport = new ReactorNettyTransport(httpClient);
```

### Vert.x (VertxHttpTransport)

```java
WebClient webClient = WebClient.create(vertx, new WebClientOptions()
    .setSsl(true)
    .setConnectTimeout(5000)
    .setMaxPoolSize(50));

// CompletableFuture — implements AsyncHttpTransport
VertxHttpTransport transport = new VertxHttpTransport(webClient);
```

### Vert.x Mutiny (VertxMutinyTransport)

```java
io.vertx.mutiny.ext.web.client.WebClient webClient = WebClient.create(vertx);

// Uni nativo — implements MutinyHttpTransport
VertxMutinyTransport transport = new VertxMutinyTransport(webClient);
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
2. A prototype-scoped `ArkClient.SyncBuilder` pre-configured with the serializer

```java
@Configuration
public class HttpClientsConfig {

    @Bean
    public Ark oauthClient(ArkClient.SyncBuilder arkBuilder) {
        return arkBuilder
            .transport(new NativeHttpTransport(HttpClient.newBuilder().build()))
            .baseUrl("https://oauth.provider.com")
            .build();
    }

    @Bean
    public Ark apiClient(ArkClient.SyncBuilder arkBuilder) {
        return arkBuilder
            .transport(new NativeHttpTransport(
                HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .connectTimeout(Duration.ofSeconds(10))
                    .build()))
            .baseUrl("https://api.myservice.com")
            .requestInterceptor(request ->
                request.header("Authorization", "Bearer " + tokenService.getToken()))
            .build();
    }
}
```

```java
@Service
public class UserService {

    private final Ark apiClient;

    public UserService(@Qualifier("apiClient") Ark apiClient) {
        this.apiClient = apiClient;
    }

    public User findById(String id) {
        return apiClient.get("/users/" + id)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(new TypeRef<User>() {});
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
            .transport(new VertxMutinyTransport(WebClient.create(vertx)))
            .baseUrl("https://api.example.com")
            .build();
    }
}

@Path("/users")
public class UserResource {

    @Inject MutinyArk client;

    @GET
    @Path("/{id}")
    public Uni<User> getUser(@PathParam("id") String id) {
        return client.get("/users/" + id)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(new TypeRef<User>() {});
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

## Architecture

Zero code duplication across execution models via two abstract base classes:

- **`AbstractClientRequest<T>`** — all fluent methods (`accept`, `contentType`, `header`, `queryParam`, `body`, `timeout`, URI building). Each subclass only adds `retrieve()`.
- **`AbstractArkClient<R>`** — all HTTP methods (`get`, `post`, `put`, `patch`, `delete`). Each subclass only implements `createRequest()`.

```
AbstractArkClient<R>
├── ArkClient          → ClientRequest          → ResponseSpec          (T)
├── AsyncArkClient     → AsyncClientRequest     → AsyncResponseSpec     (CompletableFuture<T>)
├── ReactorArkClient   → ReactorClientRequest   → ReactorResponseSpec   (Mono<T>)
└── MutinyArkClient    → MutinyClientRequest    → MutinyResponseSpec    (Uni<T>)
```

---

## Package Structure

```
ark-core:
  xyz.juandiii.ark              → Ark, ArkClient, AbstractArkClient, JsonSerializer, TypeRef
  xyz.juandiii.ark.http         → AbstractClientRequest, ClientRequest, ResponseSpec,
                                  HttpTransport, ArkResponse, RawResponse
  xyz.juandiii.ark.interceptor  → RequestContext, RequestInterceptor, ResponseInterceptor
  xyz.juandiii.ark.exceptions   → ApiException, ArkException
  xyz.juandiii.ark.type         → MediaType

ark-async:
  xyz.juandiii.ark.async        → AsyncArk, AsyncArkClient
  xyz.juandiii.ark.async.http   → AsyncHttpTransport, AsyncClientRequest, AsyncResponseSpec

ark-reactor:
  xyz.juandiii.ark.reactor      → ReactorArk, ReactorArkClient
  xyz.juandiii.ark.reactor.http → ReactorHttpTransport, ReactorClientRequest, ReactorResponseSpec

ark-mutiny:
  xyz.juandiii.ark.mutiny       → MutinyArk, MutinyArkClient
  xyz.juandiii.ark.mutiny.http  → MutinyHttpTransport, MutinyClientRequest, MutinyResponseSpec

ark-jackson:
  xyz.juandiii.ark              → JacksonSerializer

ark-transport-jdk:
  xyz.juandiii.ark.transport.jdk            → NativeHttpTransport

ark-transport-reactor:
  xyz.juandiii.ark.transport.reactor        → ReactorNettyTransport

ark-transport-vertx:
  xyz.juandiii.ark.transport.vertx          → VertxHttpTransport

ark-transport-vertx-mutiny:
  xyz.juandiii.ark.transport.vertx.mutiny   → VertxMutinyTransport

ark-spring-boot-starter:
  xyz.juandiii.spring                       → ArkAutoConfiguration
```

---

## Requirements

- Java 25+
- Jackson (for `ark-jackson`)
- Spring Boot 4.0+ (for `ark-spring-boot-starter`)
- Reactor Core (for `ark-reactor`)
- Reactor Netty (for `ark-transport-reactor`)
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
