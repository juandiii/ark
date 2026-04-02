# Ark 🛳

**A modular HTTP client toolkit for Java 17+ with fluent and declarative APIs, pluggable transports, and support for sync, async, and reactive applications.**

---

## The Problem

Java has no shortage of HTTP clients. But each one forces a trade-off:

- **JDK HttpClient** - low-level, no serialization, no interceptors, no declarative API
- **Spring WebClient / RestClient** - optimized for Spring applications and programming models
- **Quarkus REST Client** - tightly aligned with Quarkus and JAX-RS-style declarative clients
- **OkHttp / Apache HttpClient** - transport only, you build everything else yourself
- **Feign** - declarative only, no fluent API, limited reactive support

Ark gives you one client model that survives framework changes, transport changes, and execution-model changes.

## The Solution

Ark separates the concerns that other clients bundle together:

| Concern | Ark's approach |
|---------|---------------|
| **How to build requests** | Fluent API or declarative interfaces - your choice |
| **How to send them** | Pluggable transports - JDK, Reactor Netty, Vert.x, Apache |
| **How to serialize** | Pluggable serializers - Jackson, JSON-B, or your own |
| **How to execute** | Sync, async, Reactor, Mutiny, Vert.x Future - same API |
| **Where to run** | Spring Boot, Quarkus, or standalone - same code |

One mental model. Any stack. No lock-in.

## Why Ark?

- **One client model across stacks** - use Ark in Spring, Quarkus, and plain Java
- **Fluent when you want control** - explicit request composition with full access to headers, params, and body
- **Declarative when you want contracts** - `@RegisterArkClient` with Spring `@HttpExchange` or JAX-RS annotations
- **Transport-agnostic** - plug in JDK, Reactor Netty, Vert.x, or Apache HttpClient
- **Execution-model aware** - sync, async, Reactor, Mutiny, and Vert.x Future
- **Production-ready features** - TLS, retry, logging, typed exceptions, and per-client config
- **Native-image friendly** - designed to work well in GraalVM-based deployments

---

## Client Styles

Ark supports multiple ways to define HTTP clients.

### Fluent API

Use the fluent API when you want full control over request composition.

```java
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

### Declarative Clients

Define an interface with `@RegisterArkClient` and inject it directly:

```java
@RegisterArkClient(baseUrl = "${api.users.url}")
@HttpExchange("/users")
public interface UserApi {

    @GetExchange("/{id}")
    User getUser(@PathVariable String id);
}
```

```java
// Spring
public UserController(UserApi userApi) { ... }

// Quarkus
@Inject UserApi userApi;
```

Supports Spring `@HttpExchange` and JAX-RS `@Path`/`@GET`/`@POST` annotations.

### JAX-RS Example

```java
@RegisterArkClient(baseUrl = "${api.users.url}")
@Path("/users")
@Produces("application/json")
public interface UserApi {

    @GET
    @Path("/{id}")
    User getUser(@PathParam("id") String id);
}
```

```java
// Quarkus
@Inject UserApi userApi;
```

### @RegisterArkClient Attributes

| Attribute | Default | Description |
|-----------|---------|-------------|
| `configKey` | `""` | Key for per-client config in `application.properties` |
| `baseUrl` | `""` | Base URL, supports `${property}` placeholders |
| `httpVersion` | `HTTP_2` | HTTP/1.1 or HTTP/2 |
| `connectTimeout` | `10` | Connection timeout (seconds) |
| `readTimeout` | `30` | Read timeout (seconds) |
| `interceptors` | `{}` | Interceptor classes (auto-detects Request/Response) |

---

## Features

- Java 17+
- Fluent HTTP API
- Declarative HTTP clients with **Spring `@HttpExchange`** or **JAX-RS `@Path`/`@GET`**
- `@RegisterArkClient` for zero-boilerplate auto-registration and injection
- Pluggable transports (JDK, Reactor Netty, Vert.x, Apache HttpClient 5)
- Pluggable serializers (Jackson, Jackson Classic, JSON-B)
- Dedicated sync, async, Reactor, Mutiny, and Vert.x APIs
- Type-safe per-client configuration (`ArkProperties` / `@ConfigMapping`)
- Per-client interceptors and default headers via config
- Retry with exponential backoff and jitter
- TLS/SSL support (Spring SSL Bundles, Quarkus TLS Registry)
- Trust-all SSL for development
- Request/response logging (`NONE`, `BASIC`, `HEADERS`, `BODY`)
- Typed exception hierarchy (400-504 mapped to specific exceptions)
- Per-request timeout support
- HTTP/2 by default
- Spring Boot (sync + WebFlux) and Quarkus integration
- GraalVM native image support
- Easy to test and mock
---

## Execution Models

Ark provides dedicated entry points for different execution models while preserving a consistent client experience.

| Model | Client | Return Type |
|-------|--------|-------------|
| Sync | `ArkClient` | `T` |
| Async | `AsyncArkClient` | `CompletableFuture<T>` |
| Reactor | `ReactorArkClient` | `Mono<T>` |
| Mutiny | `MutinyArkClient` | `Uni<T>` |
| Vert.x | `VertxArkClient` | `Future<T>` |

Same fluent API - only the return type changes:

```java
User user = client
    .get("/users/1")
    .retrieve()
    .body(User.class);

CompletableFuture<User> cf = asyncClient
    .get("/users/1")
    .retrieve()
    .body(User.class);

Mono<User> mono = reactorClient
    .get("/users/1")
    .retrieve()
    .body(User.class);

Uni<User> uni = mutinyClient
    .get("/users/1")
    .retrieve()
    .body(User.class);

Future<User> future = vertxClient
    .get("/users/1")
    .retrieve()
    .body(User.class);
```

---

## Installation

Import the BOM first:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>xyz.juandiii</groupId>
            <artifactId>ark-bom</artifactId>
            <version>1.0.3</version> <!-- ark-bom -->
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Then choose the modules you need.

### Core + Jackson + JDK transport

```xml
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

### Optional modules

For async support:

```xml
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-async</artifactId>
</dependency>
```

For Reactor support:

```xml
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-reactor</artifactId>
</dependency>
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-transport-reactor</artifactId>
</dependency>
```

For Mutiny support:

```xml
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-mutiny</artifactId>
</dependency>
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-transport-vertx-mutiny</artifactId>
</dependency>
```

For Vert.x `Future` support:

```xml
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-vertx</artifactId>
</dependency>
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-transport-vertx</artifactId>
</dependency>
```

For Spring Boot:

```xml
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-spring-boot-starter</artifactId>
</dependency>
```

For Spring WebFlux:

```xml
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-spring-boot-starter-webflux</artifactId>
</dependency>
```

For Quarkus (Jackson):

```xml
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-quarkus-jackson</artifactId>
</dependency>
```

Auto-configures `JsonSerializer` (Jackson 2.x), `ArkClient.Builder` (sync), and `MutinyArkClient.Builder` (reactive) as CDI beans.

---

## Transport Model

Ark uses a **bridge pattern**.

The transport layer is a thin adapter around an already configured HTTP client.  
Ark does not own connection pools, low-level HTTP tuning, or TLS setup. Those concerns stay in the underlying transport.

This makes Ark flexible by design.

Built-in transports include:

- JDK `HttpClient`
- Reactor Netty
- Vert.x Web Client
- Vert.x Mutiny Web Client
- Apache HttpClient 5

You can also provide your own transport implementation.

---

## Documentation

- [Getting Started](docs/getting-started.md)
- [Sync Client](docs/sync.md)
- [Error Handling](docs/error-handling.md) - typed exception hierarchy
- [Async Client](docs/async.md)
- [Reactor Client](docs/reactor.md)
- [Mutiny Client](docs/mutiny.md)
- [Vert.x Client](docs/vertx.md)
- [Transport Model](docs/transports.md) - built-in and custom transports
- [Serialization](docs/serialization.md) - Jackson, JSON-B, custom
- [Logging](docs/logging.md) - LoggingInterceptor + TransportLogger
- [Retry & Backoff](docs/retry.md) - automatic retry with exponential backoff
- [Multipart Upload](docs/multipart.md) - file upload with binary fidelity
- [Declarative Spring Clients](docs/declarative-spring.md)
- [Declarative JAX-RS Clients](docs/declarative-jaxrs.md)
- [Spring Boot Integration](docs/spring-boot.md) - sync + WebFlux, config, TLS
- [Quarkus Integration](docs/quarkus.md)
- [Quarkus Jackson Extension](docs/quarkus-jackson.md)
- [Testing](docs/testing.md)
- [Design Principles](docs/design.md)

---

## Design Principles

- Keep transport explicit
- Keep serialization replaceable
- Support fluent and declarative styles
- Keep execution models separate
- Stay framework-friendly
- Prefer composition over lock-in

---

## Build

```bash
mvn clean install
mvn clean install -DskipTests
mvn test
```

---

## Contributing

Contributions are welcome!

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on commit conventions, PR labels, and the release process.

---

## License

Apache 2.0
