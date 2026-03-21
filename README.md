# Ark

A lightweight, fluent HTTP client library for Java 25+ with pluggable transport and serialization. Ark provides a clean three-phase API: **method → configure → retrieve → extract**, with fully separated sync (`Ark`) and async (`AsyncArk`) interfaces.

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

// Async
AsyncArk asyncClient = ArkClient.async()
    .serializer(new JacksonSerializer(new ObjectMapper()))
    .transport(new NativeHttpTransport(HttpClient.newBuilder().build()))
    .baseUrl("https://api.example.com")
    .build();

CompletableFuture<User> future = asyncClient.get("/users/1")
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .body(new TypeRef<User>() {});
```

## Modules

```
ark-core                    Core interfaces and fluent API — zero external dependencies
ark-jackson                 Jackson-based JsonSerializer implementation
ark-transport-jdk           NativeHttpTransport backed by java.net.http.HttpClient
ark-spring-boot-starter     Spring Boot auto-configuration
ark-bom                     Bill of Materials for version management
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

<dependencies>
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
</dependencies>
```

For Spring Boot projects, use the starter instead:

```xml
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-spring-boot-starter</artifactId>
</dependency>
```

---

## Building the Client

Ark uses a staged builder pattern. `ArkClient.sync()` builds sync clients, `ArkClient.async()` builds async clients. Both share the same configuration methods via `AbstractBuilder`.

### Sync

```java
Ark client = ArkClient.sync()
    .serializer(new JacksonSerializer(new ObjectMapper()))
    .transport(new NativeHttpTransport(HttpClient.newBuilder().build()))
    .baseUrl("https://api.example.com")
    .build();
```

### Async

```java
AsyncArk asyncClient = ArkClient.async()
    .serializer(new JacksonSerializer(new ObjectMapper()))
    .transport(new NativeHttpTransport(HttpClient.newBuilder().build()))
    .baseUrl("https://api.example.com")
    .build();
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

### GET

```java
User user = client.get("/users/1")
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .body(new TypeRef<User>() {});

List<User> users = client.get("/users")
    .queryParam("page", "1")
    .queryParam("size", "20")
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .body(new TypeRef<List<User>>() {});
```

### POST

```java
User created = client.post("/users")
    .contentType(MediaType.APPLICATION_JSON)
    .accept(MediaType.APPLICATION_JSON)
    .body(new User("Juan", "juan@example.com"))
    .retrieve()
    .body(new TypeRef<User>() {});
```

### PUT / PATCH

```java
User updated = client.put("/users/1")
    .contentType(MediaType.APPLICATION_JSON)
    .body(updatedUser)
    .retrieve()
    .body(new TypeRef<User>() {});

User patched = client.patch("/users/1")
    .contentType(MediaType.APPLICATION_JSON)
    .body(Map.of("name", "New Name"))
    .retrieve()
    .body(new TypeRef<User>() {});
```

### DELETE

```java
client.delete("/users/1")
    .retrieve()
    .toBodilessEntity();
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

If not set, uses the `HttpClient`'s default `connectTimeout`.

---

## Async Requests

`AsyncArk` uses the same fluent API — `.retrieve()` returns `AsyncResponseSpec` where all methods return `CompletableFuture`:

```java
CompletableFuture<User> future = asyncClient.get("/users/1")
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .body(new TypeRef<User>() {});

CompletableFuture<ArkResponse<User>> future = asyncClient.post("/users")
    .contentType(MediaType.APPLICATION_JSON)
    .body(user)
    .retrieve()
    .toEntity(new TypeRef<User>() {});

CompletableFuture<ArkResponse<Void>> future = asyncClient.delete("/users/1")
    .retrieve()
    .toBodilessEntity();
```

---

## HTTP Transport

Ark uses a **bridge pattern** — the transport is a thin adapter that wraps an already-configured HTTP client. All settings (timeouts, SSL, connection pools, HTTP version) are configured on the client itself.

Two transport interfaces:

| Interface | Method | Use case |
|-----------|--------|----------|
| `HttpTransport` | `send()` | Synchronous execution |
| `AsyncHttpTransport` | `sendAsync()` | Asynchronous via `CompletableFuture` |

A transport can implement one or both.

### Java Native (NativeHttpTransport)

Implements both `HttpTransport` and `AsyncHttpTransport`:

```java
HttpClient httpClient = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_2)
    .connectTimeout(Duration.ofSeconds(10))
    .followRedirects(HttpClient.Redirect.NORMAL)
    .sslContext(sslContext)
    .proxy(ProxySelector.of(new InetSocketAddress("proxy.corp.com", 8080)))
    .executor(Executors.newVirtualThreadPerTaskExecutor())
    .build();

NativeHttpTransport transport = new NativeHttpTransport(httpClient);

// With custom executor for async operations
NativeHttpTransport transport = new NativeHttpTransport(httpClient, myExecutor);
```

### OkHttp (sync only)

```java
OkHttpClient okHttp = new OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES))
    .build();

Ark client = ArkClient.sync()
    .serializer(serializer)
    .transport(new OkHttpTransport(okHttp))
    .baseUrl("https://api.example.com")
    .build();
```

### Reactor Netty / Vert.x (async only)

```java
AsyncArk asyncClient = ArkClient.async()
    .serializer(serializer)
    .transport(new NettyHttpTransport(reactorClient))
    .baseUrl("https://api.example.com")
    .build();
```

### Custom Transport

```java
public class MyTransport implements HttpTransport, AsyncHttpTransport {

    private final MyHttpClient client;

    public MyTransport(MyHttpClient client) {
        this.client = client;
    }

    @Override
    public RawResponse send(String method, URI uri, Map<String, String> headers,
                            String body, Duration timeout) { ... }

    @Override
    public CompletableFuture<RawResponse> sendAsync(String method, URI uri,
            Map<String, String> headers, String body, Duration timeout) { ... }
}
```

---

## Interceptors

### Request Interceptor

Runs before the HTTP call. Receives `RequestContext` — works with both sync and async requests.

```java
Ark client = ArkClient.sync()
    .serializer(serializer)
    .transport(transport)
    .baseUrl("https://api.example.com")
    .requestInterceptor(request -> {
        request.header("Authorization", "Bearer " + tokenService.getToken());
        request.header("X-Request-Id", UUID.randomUUID().toString());
    })
    .build();
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

Multiple interceptors execute in registration order. In async mode, they are chained via `CompletableFuture.thenApply`.

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
    if (e.isNotFound()) {
        // Handle 404
    } else if (e.isUnauthorized()) {
        // Handle 401
    }
    log.error("HTTP {}: {}", e.statusCode(), e.responseBody());
} catch (ArkException e) {
    log.error("Connection failed", e);
}
```

In async mode, exceptions are wrapped in `CompletionException` inside the `CompletableFuture`.

---

## Spring Boot Integration

The `ark-spring-boot-starter` auto-configures:
1. A `JsonSerializer` bean (JacksonSerializer)
2. A `NativeHttpTransport` bean (default HttpClient)
3. A prototype-scoped `ArkClient.SyncBuilder` pre-configured with serializer + transport
4. A prototype-scoped `ArkClient.AsyncBuilder` pre-configured with serializer + transport

### Configuration

```java
@Configuration
public class HttpClientsConfig {

    @Bean
    public Ark oauthClient(ArkClient.SyncBuilder arkBuilder) {
        return arkBuilder
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

    @Bean
    public AsyncArk asyncClient(ArkClient.AsyncBuilder asyncBuilder) {
        return asyncBuilder
            .baseUrl("https://api.myservice.com")
            .build();
    }
}
```

### Usage

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

## Package Structure

```
ark-core:
  xyz.juandiii.ark
  ├── Ark                              Sync entry point interface
  ├── AsyncArk                         Async entry point interface
  ├── ArkClient                        Staged builder (sync/async) + sync impl
  ├── AsyncArkClient                   Async implementation (package-private)
  ├── JsonSerializer                   Serialization SPI
  ├── TypeRef<T>                       Type-safe generic reference
  ├── exceptions/
  │   ├── ApiException                 HTTP error (status >= 400)
  │   └── ArkException                 Transport/IO error
  ├── http/
  │   ├── HttpTransport                Sync transport interface
  │   ├── AsyncHttpTransport           Async transport interface
  │   ├── ClientRequest                Sync fluent request config
  │   ├── AsyncClientRequest           Async fluent request config
  │   ├── ResponseSpec                 Sync response extraction
  │   ├── AsyncResponseSpec            Async response extraction
  │   ├── ArkResponse<T>              Response record (status + headers + body)
  │   └── RawResponse                  Raw HTTP response record
  ├── interceptor/
  │   ├── RequestContext               Shared interface for request modification
  │   ├── RequestInterceptor           Pre-request hook
  │   └── ResponseInterceptor         Post-response hook
  └── type/
      └── MediaType                    Content type constants

ark-jackson:
  xyz.juandiii.ark
  └── JacksonSerializer                Jackson ObjectMapper implementation

ark-transport-jdk:
  xyz.juandiii.ark.transport.jdk
  └── NativeHttpTransport              java.net.http.HttpClient bridge (sync + async)

ark-spring-boot-starter:
  xyz.juandiii.spring
  └── ArkAutoConfiguration            Auto-configures serializer, transport, and builders
```

---

## Requirements

- Java 25+
- Jackson (for `ark-jackson` module)
- Spring Boot 4.0+ (for `ark-spring-boot-starter` module)

## Building

```bash
mvn clean install              # Full build with tests
mvn clean install -DskipTests  # Full build without tests
mvn test                       # Run tests only
```

## License

MIT