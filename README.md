# Ark 🛳️

**A lightweight, fluent HTTP client for Java 17+ with pluggable transport, pluggable serialization, and first-class support for sync, async, and reactive applications.**

> Fluent HTTP for modern Java applications.  
> Bring your own transport. Keep your execution model. Stay type-safe.

Ark gives you a clean, consistent API to build requests, execute them through the HTTP engine you already trust, and extract responses in a type-safe way.

---

## Table of Contents

- [Why Ark?](#why-ark)
- [Core Philosophy](#core-philosophy)
- [Features](#features)
- [Supported Execution Models](#supported-execution-models)
- [Modules](#modules)
- [Quick Start](#quick-start)
- [Building a Client](#building-a-client)
- [Full Configuration Example](#full-configuration-example)
- [Making Requests](#making-requests)
- [Transport Model](#transport-model)
- [Built-in Transports](#built-in-transports)
- [Custom Transport](#custom-transport)
- [Interceptors](#interceptors)
- [Error Handling](#error-handling)
- [Spring Boot Integration](#spring-boot-integration)
- [Quarkus Integration](#quarkus-integration)
- [Custom Serializer](#custom-serializer)
- [Testing](#testing)
- [Requirements](#requirements)
- [Design Principles](#design-principles)
- [Build](#build)
- [Project Status](#project-status)
- [Contributing](#contributing)
- [License](#license)

---

## Why Ark?

Java HTTP clients often couple too many concerns into one abstraction: request building, transport, serialization, and execution style.

Ark keeps these concerns separate:

- **Fluent request API**
- **Pluggable HTTP transport**
- **Pluggable serialization**
- **Separate execution models**
- **Consistent request flow across sync, async, and reactive codebases**

Ark is **not an HTTP engine**.  
It is a **thin client abstraction** that sits on top of the transport you already use.

That means you can keep your preferred HTTP stack and still get a clean, modern developer experience.

---

## Core Philosophy

Ark is built around a simple flow:

**method → configure → retrieve → extract**

Example:

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

- **Fluent HTTP API**
- **Java 17+**
- **Pluggable transports**
- **Pluggable serializers**
- **Type-safe response extraction**
- **Dedicated sync, async, Reactor, Mutiny, and Vert.x APIs**
- **Request and response interceptors**
- **Per-request timeout support**
- **Framework-friendly integration**
- **Easy to test and mock**

---

## Supported Execution Models

Ark provides separate entry points for each execution model while preserving the same fluent style.

| Execution Model | Client Type | Return Style |
|---|---|---|
| Sync | `ArkClient` | direct value |
| Async | `AsyncArkClient` | `CompletableFuture<T>` |
| Reactor | `ReactorArkClient` | `Mono<T>` |
| Mutiny | `MutinyArkClient` | `Uni<T>` |
| Vert.x | `VertxArkClient` | `Future<T>` |

This means the request-building experience stays familiar, while the result type matches your application model.

---

## Modules

Import the BOM first:

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

Then choose only the modules you need.

### Core + Jackson + JDK transport

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

### Async support

```xml
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-async</artifactId>
</dependency>
```

### Reactor support

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

### Mutiny support

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

### Spring Boot starter

```xml
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-spring-boot-starter</artifactId>
</dependency>
```

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

---

## Transport Model

Ark uses a **bridge pattern**.

The transport layer is a thin adapter around an already configured HTTP client.  
Ark does not own connection pools, SSL setup, HTTP version selection, or low-level tuning.

Those concerns remain where they belong: in the underlying HTTP client.

### Transport interfaces

| Interface | Returns | Module |
|---|---|---|
| `HttpTransport` | `RawResponse` | `ark-core` |
| `AsyncHttpTransport` | `CompletableFuture<RawResponse>` | `ark-async` |
| `ReactorHttpTransport` | `Mono<RawResponse>` | `ark-reactor` |
| `MutinyHttpTransport` | `Uni<RawResponse>` | `ark-mutiny` |
| `VertxHttpTransport` | `Future<RawResponse>` | `ark-vertx` |

A single transport may implement multiple interfaces.

For example, `ArkJdkHttpTransport` supports both sync and async execution.

---

## Built-in Transports

### Java native HTTP client

```java
HttpClient httpClient = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_2)
    .connectTimeout(Duration.ofSeconds(10))
    .sslContext(sslContext)
    .executor(Executors.newVirtualThreadPerTaskExecutor())
    .build();

ArkJdkHttpTransport transport = new ArkJdkHttpTransport(httpClient);
```

### Reactor Netty

```java
reactor.netty.http.client.HttpClient httpClient = HttpClient.create()
    .responseTimeout(Duration.ofSeconds(30))
    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
    .secure(ssl -> ssl.sslContext(sslContext));

ArkReactorNettyTransport transport = new ArkReactorNettyTransport(httpClient);
```

### Vert.x with `CompletableFuture`

```java
WebClient webClient = WebClient.create(vertx, new WebClientOptions()
    .setSsl(true)
    .setConnectTimeout(5000)
    .setMaxPoolSize(50));

ArkVertxTransport transport = new ArkVertxTransport(webClient);
```

### Vert.x with native `Future`

```java
ArkVertxFutureTransport transport = new ArkVertxFutureTransport(webClient);
```

### Vert.x Mutiny

```java
io.vertx.mutiny.ext.web.client.WebClient webClient = WebClient.create(vertx);

ArkVertxMutinyTransport transport = new ArkVertxMutinyTransport(webClient);
```

### Apache HttpClient 5

```java
CloseableHttpClient httpClient = HttpClients.custom()
    .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
        .setMaxConnTotal(100)
        .setMaxConnPerRoute(20)
        .build())
    .setDefaultRequestConfig(RequestConfig.custom()
        .setResponseTimeout(Timeout.ofSeconds(30))
        .build())
    .build();

ArkApacheTransport transport = new ArkApacheTransport(httpClient);
```

---

## Custom Transport

Bringing your own transport is intentionally simple.

```java
public class MyTransport implements HttpTransport {

    private final MyHttpClient client;

    public MyTransport(MyHttpClient client) {
        this.client = client;
    }

    @Override
    public RawResponse send(
            String method,
            URI uri,
            Map<String, String> headers,
            String body,
            Duration timeout
    ) {
        // Adapt your client's API and return RawResponse
    }
}
```

This makes Ark easy to integrate with existing infrastructure and internal HTTP abstractions.

---

## Interceptors

### Request interceptor

Runs before the request is executed.

```java
.requestInterceptor(request -> {
    request.header("Authorization", "Bearer " + tokenService.getToken());
    request.header("X-Request-Id", UUID.randomUUID().toString());
})
```

The interceptor is evaluated on every request, so dynamic values such as tokens or correlation IDs stay fresh.

### Response interceptor

Runs after the HTTP call and can inspect or transform the response.

```java
.responseInterceptor(response -> {
    log.info("HTTP {} - {} bytes", response.statusCode(), response.body().length());
    return response;
})
```

Multiple interceptors run in registration order.

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

---

## Spring Boot Integration

`ark-spring-boot-starter` auto-configures:

- a `JsonSerializer` bean
- a prototype-scoped `ArkClient.Builder`

Example:

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

---

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

You can plug in your own serializer implementation.

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

## Testing

Ark is easy to test because transport is an explicit dependency.

You can plug in a fake or mock transport without starting a server or mocking static client code.

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

This makes Ark a strong fit for unit testing and library development.

---

## Requirements

- Java 17+
- Jackson for `ark-jackson`
- Spring Boot 4.0+ for `ark-spring-boot-starter`
- Reactor Core for `ark-reactor`
- Reactor Netty for `ark-transport-reactor`
- Vert.x Core for `ark-vertx`
- Vert.x Web Client for Vert.x transports
- SmallRye Mutiny + Vert.x Mutiny for `ark-mutiny`
- Apache HttpClient 5 for `ark-transport-apache`

---

## Design Principles

- **Do not own the HTTP stack**
- **Keep transport explicit**
- **Keep serialization replaceable**
- **Keep execution models separate**
- **Keep the fluent API consistent**
- **Prefer composition over framework lock-in**

---

## Build

```bash
mvn clean install
mvn clean install -DskipTests
mvn test
```

---

## Project Status

Ark is currently evolving as a modular HTTP client focused on API clarity, execution-model separation, and transport flexibility.

Feedback, ideas, and contributions are welcome.

---

## Contributing

Contributions are welcome.

If you want to help, feel free to open an issue or submit a pull request for:

- new transports
- documentation improvements
- framework integrations
- bug fixes
- performance improvements
- test coverage

---

## License

Apache 2.0

