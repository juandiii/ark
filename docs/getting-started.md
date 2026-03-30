# Getting Started

---

## Installation

Import the BOM:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>xyz.juandiii</groupId>
            <artifactId>ark-bom</artifactId>
            <version>1.0.9-SNAPSHOT</version> <!-- ark-bom -->
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Add the modules you need:

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

Or use a starter:

| Starter | Stack | Features |
|---------|-------|----------|
| `ark-spring-boot-starter` | Spring MVC (sync) | Config, TLS, retry, interceptors, headers, native |
| `ark-spring-boot-starter-webflux` | Spring WebFlux (reactive) | Config, TLS, interceptors, headers, native |
| `ark-quarkus-jackson` | Quarkus (sync + Mutiny) | Config, TLS, retry, interceptors, headers, native |

---

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
    .serializer(serializer)
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
    .serializer(serializer)
    .transport(new ArkReactorNettyTransport(HttpClient.create()))
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
    .transport(new ArkVertxMutinyTransport(WebClient.create(vertx)))
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
    .transport(new ArkVertxFutureTransport(WebClient.create(vertx)))
    .baseUrl("https://api.example.com")
    .build();

Future<User> user = client.get("/users/1")
    .retrieve()
    .body(User.class);
```

---

## Making Requests

### Response extraction

```java
// Class<T> — simple types
User user = client.get("/users/1").retrieve().body(User.class);
String html = client.get("/health").retrieve().body(String.class);

// TypeRef<T> — generic types
List<User> users = client.get("/users").retrieve().body(new TypeRef<List<User>>() {});
```

### Query parameters

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
client.delete("/users/1").retrieve().toBodilessEntity();
```

### Full response (status + headers + body)

```java
ArkResponse<User> response = client.get("/users/1")
    .retrieve()
    .toEntity(User.class);

int status = response.statusCode();
User body = response.body();
boolean ok = response.isSuccessful();
```

### Per-request timeout

```java
User user = client.get("/slow-endpoint")
    .timeout(Duration.ofSeconds(120))
    .retrieve()
    .body(User.class);
```

---

## Full Configuration

```java
HttpClient httpClient = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_2)
    .connectTimeout(Duration.ofSeconds(10))
    .followRedirects(HttpClient.Redirect.NORMAL)
    .sslContext(mySSLContext)
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

## Logging

```properties
ark.logging.level=BODY
```

Levels: `NONE`, `BASIC`, `HEADERS`, `BODY`. See [Logging](logging.md) for full details.

---

## Declarative Clients

Define an interface — Ark creates the implementation:

```java
@RegisterArkClient(configKey = "user-api", baseUrl = "${api.users.url}")
@Path("/users")
@Produces("application/json")
public interface UserApi {
    @GET @Path("/{id}")
    User getUser(@PathParam("id") String id);
}
```

Configure per-client via `application.properties`:

```properties
ark.client.user-api.base-url=https://api.example.com
ark.client.user-api.http-version=HTTP_2
```

See [Declarative JAX-RS](declarative-jaxrs.md) or [Declarative Spring](declarative-spring.md) for full details.

---

## Same API, Different Return Types

```java
User user                    = client.get("/users/1").retrieve().body(User.class);
CompletableFuture<User> cf   = asyncClient.get("/users/1").retrieve().body(User.class);
Mono<User> mono              = reactorClient.get("/users/1").retrieve().body(User.class);
Uni<User> uni                = mutinyClient.get("/users/1").retrieve().body(User.class);
Future<User> future          = vertxClient.get("/users/1").retrieve().body(User.class);
```

---

## Next Steps

- [Sync Client](sync.md)
- [Error Handling](error-handling.md) — typed exception hierarchy
- [Reactor Client](reactor.md) — Spring WebFlux
- [Mutiny Client](mutiny.md) — Quarkus
- [Transport Model](transports.md) — built-in and custom transports
- [Serialization](serialization.md) — Jackson, JSON-B, custom
- [Logging](logging.md) — LoggingInterceptor + TransportLogger
- [Retry & Backoff](retry.md) — automatic retry with exponential backoff
- [Declarative JAX-RS](declarative-jaxrs.md) — `@RegisterArkClient` with JAX-RS
- [Declarative Spring](declarative-spring.md) — `@RegisterArkClient` with `@HttpExchange`
- [Spring Boot Integration](spring-boot.md) — auto-config, ArkProperties, TLS
- [Quarkus Integration](quarkus.md) — @ConfigMapping, TLS
- [Testing](testing.md)