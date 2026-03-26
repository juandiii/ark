# Ark 🛳️

**A modular HTTP client toolkit for Java 17+ with fluent and declarative APIs, pluggable transports, pluggable serialization, and first-class support for sync, async, and reactive applications.**

> Fluent when you want control. Declarative when you want contracts.

Ark lets you build HTTP clients in the style that fits your application:

- **Fluent API** for explicit request composition
- **Declarative clients** with Spring `@HttpExchange` or JAX-RS `@Path`/`@GET`/`@POST` annotations
- **Auto-registration** with `@RegisterArkClient` — zero boilerplate injection

All while keeping transport, serialization, and execution model explicit and reusable.

---

## Why Ark?

Java HTTP clients often force you into one specific style, one framework, or one transport model.

Ark takes a different approach:

- Use a **fluent API** when you want explicit request control
- Use **declarative interfaces** when you want contract-first clients
- Keep your **HTTP transport pluggable**
- Keep your **serializer replaceable**
- Choose the **execution model** that matches your stack
- Reuse the same mental model across Spring, Quarkus, and plain Java

Ark is not just an HTTP engine.  
It is a **client toolkit** for building HTTP clients in a consistent, framework-friendly, and transport-agnostic way.

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
public UserController(@ArkClient UserApi userApi) { ... }
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
public UserController(@ArkClient UserApi userApi) { ... }
```

### @RegisterArkClient Attributes

| Attribute | Default | Description |
|-----------|---------|-------------|
| `baseUrl` | `""` | Base URL, supports `${property}` placeholders |
| `httpVersion` | `HTTP_1_1` | HTTP/1.1 or HTTP/2 |
| `connectTimeout` | `10` | Connection timeout (seconds) |
| `readTimeout` | `30` | Read timeout (seconds) |

---

## Features

- Java 17+
- Fluent HTTP API
- Declarative HTTP clients with **Spring `@HttpExchange`** or **JAX-RS `@Path`/`@GET`**
- `@RegisterArkClient` for zero-boilerplate auto-registration and injection
- Pluggable transports
- Pluggable serializers
- Dedicated sync, async, Reactor, Mutiny, and Vert.x APIs
- Request and response interceptors
- Per-request timeout support
- Spring Boot and Quarkus integration
- Easy to test and mock
- Modular Maven structure

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

Same fluent API — only the return type changes:

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
            <version>1.0.9-SNAPSHOT</version> <!-- ark-bom -->
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

## Serialization

Serialization is explicit and replaceable.

Use Jackson, Gson, or your own serializer implementation without changing the shape of your clients.

```java
Ark client = ArkClient.builder()
    .serializer(new JacksonSerializer(new ObjectMapper()))
    .transport(new ArkJdkHttpTransport(HttpClient.newBuilder().build()))
    .baseUrl("https://api.example.com")
    .build();
```

---

## Testing

Ark is easy to test because transport is an explicit dependency.

You can plug in a fake transport without starting a server or mocking a concrete HTTP client.

```java
HttpTransport transport = (method, uri, headers, body, timeout) ->
    new RawResponse(
        200,
        Map.of("Content-Type", List.of("application/json")),
        "{\"id\":1,\"name\":\"Juan\"}"
    );

Ark client = ArkClient.builder()
    .serializer(new JacksonSerializer(new ObjectMapper()))
    .transport(transport)
    .baseUrl("https://api.example.com")
    .build();

User user = client.get("/users/1")
    .retrieve()
    .body(User.class);
```

---

## Documentation

- [Getting Started](docs/getting-started.md)
- [Transport Model](docs/transports.md)
- [Declarative Spring Clients](docs/declarative-spring.md)
- [Declarative JAX-RS Clients](docs/declarative-jaxrs.md)
- [Sync Client](docs/sync.md)
- [Async Client](docs/async.md)
- [Reactor Client](docs/reactor.md)
- [Mutiny Client](docs/mutiny.md)
- [Vert.x Client](docs/vertx.md)
- [Spring Boot Integration](docs/spring-boot.md)
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
