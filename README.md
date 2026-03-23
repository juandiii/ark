# Ark 🛳️

**A lightweight, fluent HTTP client for Java 17+ with pluggable transport, pluggable serialization, and first-class support for sync, async, and reactive applications.**

> Fluent HTTP for modern Java applications.  
> Bring your own transport. Keep your execution model. Stay type-safe.

---

## Why Ark?

Java HTTP clients often mix transport, serialization, and execution style into one abstraction.

Ark keeps them separate:

- Bring your own HTTP transport
- Choose your execution model
- Keep a consistent fluent API
- Stay easy to test and evolve

---

## Core Philosophy

**method → configure → retrieve → extract**

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

---

## Features

- Java 17+
- Fluent HTTP API
- Pluggable transports (JDK, Reactor Netty, Vert.x, Apache HttpClient 5)
- Pluggable serializers (Jackson, Gson, custom)
- Dedicated sync, async, Reactor, Mutiny, and Vert.x APIs
- Request and response interceptors
- Per-request timeout
- Spring Boot and Quarkus integration
- Easy to test and mock

---

## Documentation

- [Getting Started](docs/getting-started.md) — Quick start, building clients, making requests
- [Transport Model](docs/transports.md) — Bridge pattern, built-in transports, custom transport
- [Sync Client](docs/sync.md) — ArkClient usage and error handling
- [Async Client](docs/async.md) — AsyncArkClient with CompletableFuture
- [Reactor Client](docs/reactor.md) — ReactorArkClient with Mono/Flux
- [Mutiny Client](docs/mutiny.md) — MutinyArkClient with Uni
- [Vert.x Client](docs/vertx.md) — VertxArkClient with Future
- [Spring Boot Integration](docs/spring-boot.md) — Starter auto-configuration
- [Quarkus Integration](docs/quarkus.md) — CDI producer setup
- [Testing](docs/testing.md) — Mock transports and unit testing
- [Design Principles](docs/design.md) — Architecture, modules, requirements

---

## Execution Models

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

### Starter Modules

For Spring Boot:

```xml
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-spring-boot-starter</artifactId>
</dependency>
```

For WebFlux:

```xml
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-spring-boot-starter-webflux</artifactId>
</dependency>
```

---

## Build

```bash
mvn clean install
mvn clean install -DskipTests
mvn test
```

---

## Contributing

Contributions are welcome — new transports, documentation, framework integrations, bug fixes, and test coverage.

---

## License

Apache 2.0
