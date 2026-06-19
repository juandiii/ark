# Ark 🛳

[![Maven Central](https://img.shields.io/maven-central/v/xyz.juandiii/ark-core?label=Maven%20Central)](https://central.sonatype.com/namespace/xyz.juandiii)
[![CI](https://github.com/juandiii/ark/actions/workflows/ci.yml/badge.svg)](https://github.com/juandiii/ark/actions/workflows/ci.yml)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://adoptium.net/)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

**A modular HTTP client toolkit for Java 17+ with fluent and declarative APIs, pluggable transports, composable decorators, and support for sync, async, and reactive applications.**

> **Status**: Ark is in active 1.x development. Breaking changes are acceptable in minor versions until 2.0. See [CHANGELOG.md](CHANGELOG.md) for what's new and migration notes per release.

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
| **How to compose behavior** | Decorators chain via `transport.with(...)` - retry, metrics, your own |
| **Where to run** | Spring Boot, Quarkus, or standalone - same code |

One mental model. Any stack. No lock-in.

## Why Ark?

- **One client model across stacks** - use Ark in Spring, Quarkus, and plain Java
- **Fluent when you want control** - explicit request composition with full access to headers, params, and body
- **Declarative when you want contracts** - `@RegisterArkClient` with Spring `@HttpExchange` or JAX-RS annotations
- **Transport-agnostic** - plug in JDK, Reactor Netty, Vert.x, or Apache HttpClient
- **Execution-model aware** - sync, async, Reactor, Mutiny, and Vert.x Future
- **Generic `Transport<R>` contract** - every execution model implements the same interface parameterized on its return wrapper
- **Composable decorators** - `transport.with(Retry.of(policy, ops))` chain works across all 5 models; bring your own (`Metrics`, `Cache`, `CircuitBreaker`)
- **Production-ready features** - TLS, retry, redacted logging, typed exceptions, per-client config
- **Async stacktraces include the caller site** - no more "lost" call frames in `CompletableFuture` failures
- **Native-image friendly** - designed to work well in GraalVM-based deployments

---

## Client Styles

Ark supports multiple ways to define HTTP clients.

### Fluent API

Use the fluent API when you want full control over request composition.

```java
import org.springframework.http.MediaType;        // or jakarta.ws.rs.core.MediaType
import xyz.juandiii.ark.core.Ark;
import xyz.juandiii.ark.core.ArkClient;
import xyz.juandiii.ark.jackson.JacksonSerializer;
import xyz.juandiii.ark.transport.jdk.ArkJdkSyncTransport;
import java.net.http.HttpClient;

Ark client = ArkClient.builder()
    .serializer(new JacksonSerializer(new ObjectMapper()))
    .transport(new ArkJdkSyncTransport(HttpClient.newBuilder().build()))
    .baseUrl("https://api.example.com")
    .build();

User user = client.get("/users/1")
    .accept(MediaType.APPLICATION_JSON_VALUE)
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

### Spring Async — same `@RegisterArkClient`

If any method on the interface returns `CompletableFuture<T>`, the Spring starter auto-wires an `AsyncArkClient` proxy instead of `ArkClient`. Zero extra configuration:

```java
@RegisterArkClient(configKey = "users-api")
@HttpExchange("/users")
public interface UserApi {

    @GetExchange("/{id}")
    CompletableFuture<User> getUser(@PathVariable String id);   // ← async
}
```

> **IDE hint**: if IntelliJ reports `Could not autowire. No beans of 'UserApi' type found.`, the bean **does** exist at runtime — Ark registers it dynamically. Add `@org.springframework.stereotype.Component` on the interface alongside `@RegisterArkClient` to silence the warning. Spring's default scan skips interfaces, so no double-registration. See [docs/spring-boot.md → IDE autowiring hint](docs/spring-boot.md#ide-autowiring-hint).

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

## Composable Decorators

Every `Transport<R>` exposes a `.with(...)` method that composes decorators. Built-in decorator: `Retry<R>`. Custom decorators (metrics, caching, circuit breaker) plug in the same way.

```java
import xyz.juandiii.ark.core.http.decorator.Retry;
import xyz.juandiii.ark.core.http.decorator.SyncRetryOps;

Transport<RawResponse> resilient = new ArkJdkSyncTransport(HttpClient.newBuilder().build())
    .with(Retry.of(retryPolicy, new SyncRetryOps()))
    // .with(MyMetrics.of(registry))
    // .with(MyCache.of(store));

Ark client = ArkClient.builder()
    .serializer(serializer)
    .transport(resilient)
    .baseUrl("https://api.example.com")
    .build();
```

The chain composes **outside-in** — the last `.with(...)` is the outermost wrapper. `RetryOps<R>` strategies exist per execution model: `SyncRetryOps`, `AsyncRetryOps`, `ReactorRetryOps`, `MutinyRetryOps`, `VertxRetryOps`. See [Retry & Backoff](docs/retry.md) for ordering rules (e.g., `Metrics` outside `Retry` measures total wall-clock; inside, per-attempt).

---

## Features

- Java 17+
- Fluent HTTP API
- Declarative HTTP clients with **Spring `@HttpExchange`** or **JAX-RS `@Path`/`@GET`**
- `@RegisterArkClient` for zero-boilerplate auto-registration and injection (sync + async)
- Generic `Transport<R>` contract unified across all 5 execution models
- Composable `.with(...)` decorator chain (built-in `Retry`; bring your own)
- Pluggable transports (JDK, Reactor Netty, Vert.x, Apache HttpClient 5)
- Pluggable serializers (Jackson, Jackson Classic, JSON-B)
- Dedicated sync, async, Reactor, Mutiny, and Vert.x APIs
- Type-safe per-client configuration (`ArkProperties` / `@ConfigMapping`)
- Per-client interceptors and default headers via config
- Retry with exponential backoff and jitter (`Retry<R>` decorator)
- Async stacktraces preserve the caller site (suppressed exception, no lost frames)
- TLS/SSL support (Spring SSL Bundles, Quarkus TLS Registry)
- Trust-all SSL for development (with runtime warning)
- Request/response logging with sensitive-header and credential-body redaction (`NONE`, `BASIC`, `HEADERS`, `BODY`)
- Typed exception hierarchy (400-504 mapped to specific exceptions)
- Per-request timeout support
- HTTP/2 by default
- Spring Boot (sync + async + WebFlux) and Quarkus integration
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
            <version>1.0.6</version> <!-- ark-bom -->
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

The transport layer is a thin adapter around an already configured HTTP client. Ark does not own connection pools, low-level HTTP tuning, or TLS setup. Those concerns stay in the underlying transport.

All transports implement a single generic contract `Transport<R>` where `R` is the return-type wrapper for the execution model. Decorators compose via `transport.with(...)`.

Built-in transports include:

- JDK `HttpClient` — split into `ArkJdkSyncTransport` (sync) and `ArkJdkAsyncTransport` (CompletableFuture); both can share the same underlying `HttpClient` for a shared connection pool
- Reactor Netty (`ArkReactorNettyTransport`)
- Vert.x Web Client (`ArkVertxFutureTransport`)
- Vert.x Mutiny Web Client (`ArkVertxMutinyTransport`)
- Apache HttpClient 5 (`ArkApacheTransport`)

You can also provide your own transport implementation or custom decorator — see [Transport Model](docs/transports.md).

---

## Logging

Ark logs requests and responses via `LoggingInterceptor` (sensitive headers and known credential body keys are redacted). Enable it per-client with `ark.logging.level=BASIC|HEADERS|BODY` (Spring / Quarkus) or programmatically via `LoggingInterceptor.apply(builder, Level.HEADERS)`.

For raw wire-level transport debugging, enable the underlying client's own logger:

- **JDK HttpClient**: `-Djdk.httpclient.HttpClient.log=all`
- **Apache HttpClient 5**: set `org.apache.hc.client5.http` to DEBUG
- **Reactor Netty**: set `reactor.netty.http.client.HttpClient` to DEBUG
- **Vert.x WebClient**: set `io.vertx.core.http.impl` to DEBUG

---

## Security

Found a vulnerability? Please follow the disclosure process in [SECURITY.md](SECURITY.md) — do not open a public issue.

### TLS

Ark validates TLS certificates by default. To use a custom truststore (self-signed CA, mutual TLS), configure your SSL bundle (Spring) or TLS configuration (Quarkus) and reference it via `ark.client.<name>.tls-configuration-name`.

> ⚠️ **`trust-all: true` disables ALL certificate validation.** Use only in local development against ephemeral environments. Setting `ark.client.<name>.trust-all=true` in production exposes your application to man-in-the-middle attacks. Ark logs a runtime WARNING when trust-all is active so accidental production use is visible.

---

## Documentation

- [CHANGELOG](CHANGELOG.md) - release notes and migration guidance
- [Getting Started](docs/getting-started.md)
- [Sync Client](docs/sync.md)
- [Async Client](docs/async.md)
- [Reactor Client](docs/reactor.md)
- [Mutiny Client](docs/mutiny.md)
- [Vert.x Client](docs/vertx.md)
- [Transport Model](docs/transports.md) - `Transport<R>` contract, built-in transports, custom transports, `.with(...)` decorator chain
- [Retry & Backoff](docs/retry.md) - `Retry<R>` + per-model `RetryOps<R>` strategies; native operator alternatives
- [Serialization](docs/serialization.md) - Jackson, JSON-B, custom
- [Logging](docs/logging.md) - `LoggingInterceptor` with redaction, wire-level escape hatches
- [Multipart Upload](docs/multipart.md) - file upload with binary fidelity
- [Error Handling](docs/error-handling.md) - typed exception hierarchy
- [Declarative Spring Clients](docs/declarative-spring.md)
- [Declarative JAX-RS Clients](docs/declarative-jaxrs.md)
- [Spring Boot Integration](docs/spring-boot.md) - sync + async + WebFlux, config, TLS, IDE autowiring hint
- [Quarkus Integration](docs/quarkus.md)
- [Quarkus Jackson Extension](docs/quarkus-jackson.md)
- [Testing](docs/testing.md)
- [Design Principles](docs/design.md)

---

## Design Principles

- Keep transport explicit
- Keep serialization replaceable
- Support fluent and declarative styles
- Keep execution models separate at the API surface, unified at the transport contract
- Stay framework-friendly
- Prefer composition over lock-in (`transport.with(...)`)

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
