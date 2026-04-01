# Design

## Design Principles

- **Do not own the HTTP stack**
- **Keep transport explicit**
- **Keep serialization replaceable**
- **Keep execution models separate**
- **Keep the fluent API consistent**
- **Prefer composition over framework lock-in**

---

## Architecture Overview

Ark is built around a simple flow:

**method -> configure -> retrieve -> extract**

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
            <version>1.0.2</version> <!-- ark-bom -->
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

### Declarative proxy (Spring)

```xml
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-proxy-spring</artifactId>
</dependency>
```

### Declarative proxy (JAX-RS)

```xml
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-proxy-jaxrs</artifactId>
</dependency>
```

Annotate your interface with `@RegisterArkClient` and inject it directly:

```java
@RegisterArkClient(baseUrl = "${api.users.url}")
@Path("/users")   // or @HttpExchange("/users")
interface UserApi { ... }

// Quarkus
@Inject UserApi userApi;

// Spring
public MyController(UserApi userApi) { ... }
```

Or create manually with `ArkProxy.create()`:

```java
UserApi api = ArkProxy.create(UserApi.class, ark);           // sync
UserApi api = ArkProxy.create(UserApi.class, asyncArk);      // CompletableFuture
UserApi api = ArkProxy.create(UserApi.class, reactorArk);    // Mono/Flux
UserApi api = ArkProxy.create(UserApi.class, mutinyArk);     // Uni/Multi
```

### @RegisterArkClient attributes

| Attribute | Default | Description |
|-----------|---------|-------------|
| `configKey` | `""` | Key for per-client config in `application.properties` |
| `baseUrl` | `""` | Base URL, supports `${property}` placeholders |
| `httpVersion` | `HTTP_2` | HTTP/1.1 or HTTP/2 |
| `connectTimeout` | `10` | Connection timeout (seconds) |
| `readTimeout` | `30` | Read timeout (seconds) |
| `interceptors` | `{}` | Interceptor classes (auto-detects `RequestInterceptor` / `ResponseInterceptor`) |

### Exception hierarchy

```
ArkException (transport/IO errors)
  +-- TimeoutException
  +-- ConnectionException
  +-- RequestInterruptedException

ApiException (HTTP status >= 400)
  +-- ClientException (4xx)
  |     +-- BadRequestException, UnauthorizedException, ForbiddenException,
  |         NotFoundException, ConflictException, UnprocessableEntityException,
  |         TooManyRequestsException
  +-- ServerException (5xx)
        +-- InternalServerErrorException, BadGatewayException,
            ServiceUnavailableException, GatewayTimeoutException
```

### Logging

`LoggingInterceptor` provides paired request/response logging at four levels: `NONE`, `BASIC`, `HEADERS`, `BODY`.

Applied via `LoggingInterceptor.apply(builder, level)` or `ark.logging.level` in `application.properties`.

### Retry

`RetryTransport` / `RetryAsyncTransport` - transport decorators with exponential backoff + jitter. Configured per-client via `ark.client.*.retry.*` properties. Only retries idempotent methods by default. Does not apply to reactive transports (Reactor/Mutiny have built-in retry).

### Multipart Upload

`MultipartBody` builder with sealed `Part` hierarchy (`FilePart`, `FieldPart`). `MultipartEncoder` encodes to `byte[]` with RFC 2046 MIME boundaries. All 5 transports override `sendBinary` natively. `ContentTypeDetector` uses magic bytes + `MimeType` enum for content type detection. `@RequestPart` annotation for declarative proxy support.

---

## Requirements

- Java 17+
- Jackson for `ark-jackson`
- Spring Boot 4.0+ for `ark-spring-boot-starter` and `ark-spring-boot-starter-webflux`
- Reactor Core for `ark-reactor`
- Reactor Netty for `ark-transport-reactor`
- Vert.x Core for `ark-vertx`
- Vert.x Web Client for Vert.x transports
- SmallRye Mutiny + Vert.x Mutiny for `ark-mutiny`
- Apache HttpClient 5 for `ark-transport-apache`