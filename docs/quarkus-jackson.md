# Quarkus Jackson Extension

The `ark-quarkus-jackson` extension auto-configures Ark HTTP clients for Quarkus applications using Jackson 2.x for serialization.

---

## Which extension?

Ark ships two Quarkus extensions:

| Extension | Includes | When to pick |
|---|---|---|
| `ark-quarkus-jackson` | JDK transport (sync + async), Jackson, TLS registry, proxy scanning | Default. Lean, no Vert.x, smaller native images. |
| `ark-quarkus-jackson-vertx` | Everything above PLUS Vert.x Mutiny transport, Vert.x TLS resolver, Mutiny client builder | Add when you have proxy methods returning `Uni<T>`/`Multi<T>` or you want the native Vert.x event-loop integration. |

Add exactly ONE of the two:

```xml
<!-- Slim -->
<dependency>
  <groupId>xyz.juandiii</groupId>
  <artifactId>ark-quarkus-jackson</artifactId>
</dependency>

<!-- OR: JDK + Vert.x Mutiny -->
<dependency>
  <groupId>xyz.juandiii</groupId>
  <artifactId>ark-quarkus-jackson-vertx</artifactId>
</dependency>
```

The vertx add-on transitively depends on the slim extension — you don't need both.

---

## Installation

Requires Quarkus 3.33 LTS or newer.

```xml
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-quarkus-jackson</artifactId>
</dependency>
```

---

## What It Provides

The slim `ark-quarkus-jackson` extension registers the following CDI beans:

| Bean | Scope | Description |
|------|-------|-------------|
| `JsonSerializer` | `@Singleton` | `JacksonClassicSerializer` using Quarkus-managed `ObjectMapper` |
| `HttpTransport` | `@Singleton` | `ArkJdkSyncTransport` with default `HttpClient` |
| `ArkClient.Builder` | `@Dependent` | Pre-configured sync builder |

Add the `ark-quarkus-jackson-vertx` add-on to additionally get:

| Bean | Scope | Description |
|------|-------|-------------|
| `MutinyHttpTransport` | `@Singleton` | `ArkVertxMutinyTransport` with Quarkus-managed `Vertx` |
| `MutinyArkClient.Builder` | `@Dependent` | Pre-configured Mutiny builder |

All beans use `@DefaultBean` - define your own to override any of them.

---

## Sync Usage

```java
@ApplicationScoped
public class UserService {

    private final Ark client;

    @Inject
    public UserService(ArkClient.Builder builder) {
        this.client = builder
            .baseUrl("https://api.example.com")
            .build();
    }

    public User getUser(String id) {
        return client.get("/users/" + id)
            .retrieve()
            .body(User.class);
    }
}
```

---

## Mutiny Usage

```java
@ApplicationScoped
public class ReactiveUserService {

    private final MutinyArk client;

    @Inject
    public ReactiveUserService(MutinyArkClient.Builder builder) {
        this.client = builder
            .baseUrl("https://api.example.com")
            .build();
    }

    public Uni<User> getUser(String id) {
        return client.get("/users/" + id)
            .retrieve()
            .body(User.class);
    }
}
```

---

## Multiple Clients

Builders are `@Dependent` scope - each injection gets a fresh instance:

```java
@ApplicationScoped
public class HttpClients {

    private final Ark oauthClient;
    private final MutinyArk apiClient;

    @Inject
    public HttpClients(ArkClient.Builder syncBuilder,
                       MutinyArkClient.Builder mutinyBuilder) {
        this.oauthClient = syncBuilder
            .baseUrl("https://oauth.provider.com")
            .build();
        this.apiClient = mutinyBuilder
            .baseUrl("https://api.myservice.com")
            .requestInterceptor(req ->
                req.header("Authorization", "Bearer " + resolveToken()))
            .build();
    }
}
```

---

## Custom HttpTransport

The default transport uses Java's `HttpClient`. You can override it or use a different transport:

### JDK HttpClient (default)

```java
@Produces
@Singleton
public HttpTransport jdkTransport() {
    return new ArkJdkSyncTransport(HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .connectTimeout(Duration.ofSeconds(10))
        .build());
}
```

### Apache HttpClient 5

Add the dependency:

```xml
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-transport-apache</artifactId>
</dependency>
```

Then override the transport:

```java
@Produces
@Singleton
public HttpTransport apacheTransport() {
    return new ArkApacheTransport(HttpClients.custom()
        .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
            .setMaxConnTotal(100)
            .setMaxConnPerRoute(20)
            .build())
        .build());
}
```

---

## Configuration (`@ConfigMapping`)

The extension provides type-safe configuration via Quarkus `@ConfigMapping`:

```properties
# application.properties

# Global logging level: NONE, BASIC, HEADERS, BODY
ark.logging.level=BODY

# Per-client configuration (key matches @RegisterArkClient configKey)
ark.client."user-api".base-url=https://api.example.com
ark.client."user-api".http-version=HTTP_2
ark.client."user-api".connect-timeout=5
ark.client."user-api".read-timeout=15
ark.client."user-api".tls-configuration-name=my-cert
ark.client."user-api".trust-all=false
ark.client."user-api".throw-on-error=true
ark.client."user-api".follow-redirects=true
ark.client."user-api".max-response-bytes=20971520
ark.client."user-api".headers.X-Api-Key=${API_KEY}
ark.client."user-api".retry.max-attempts=3
ark.client."user-api".retry.delay=500
```

> ⚠️ `trust-all=true` disables certificate validation. Use only in local development. See [Security & TLS in README](../README.md#tls).

> Set `throw-on-error=false` to return HTTP 4xx/5xx responses instead of raising `ApiException`. See [Permissive error handling](sync.md#permissive-error-handling).

> `follow-redirects` (default `true`) makes the default JDK `HttpClient` follow 3xx redirects, except https to http downgrades. Set it to `false` to receive 3xx responses as-is. If you supply your own `HttpClient`/`HttpTransport` bean, you set the redirect policy yourself (`HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL)`).

> `max-response-bytes` (default `0`, unlimited) caps the response body size. Larger bodies fail with `ResponseTooLargeException`.

See [Retry & Backoff](retry.md) for full retry configuration.

```java
@RegisterArkClient(configKey = "user-api")
@Path("/users")
@Produces("application/json")
public interface UserApi {
    @GET @Path("/{id}")
    Uni<User> findById(@PathParam("id") Long id);
}
```

Properties take precedence over annotation values. Quarkus map keys use quoted format: `ark.client."key-name".property`.

---

## Fluent clients with named configuration

The `ark.client."<name>".*` properties also configure fluent (non-declarative) clients. Inject `ArkClients` and ask for a builder by key:

```java
@ApplicationScoped
public class RatesFetcher {
    private final Ark client;

    RatesFetcher(ArkClients arkClients) {
        this.client = arkClients.builder("fxrates-sources").build();
    }
}
```

```properties
ark.client."fxrates-sources".base-url=https://api.example.com
ark.client."fxrates-sources".connect-timeout=10
ark.client."fxrates-sources".read-timeout=60
ark.client."fxrates-sources".max-response-bytes=20971520
```

Base URL, timeouts, TLS, headers, retry, redirects, `throw-on-error` and `max-response-bytes` are applied. Use `arkClients.asyncBuilder("name")` for the `CompletableFuture` variant (`AsyncArkClient.Builder`).

`retrieveStream()` works with `ArkClients` builders (also when retry is configured) and honors `max-response-bytes`; see [Streaming responses](sync.md#streaming-responses-large-downloads).

Each `builder(...)` call returns a fresh builder, so you can keep customizing it before `build()`. An unknown key throws `ArkException`.

Each call also creates a new underlying HTTP client (with its own connection pool and threads). Build the client once, for example in a field initialized in the constructor or in a `@PostConstruct` method of an `@ApplicationScoped` bean, and reuse it. Do not call `builder(...)` per request.

---

## TLS / SSL

Configure TLS via Quarkus TLS Registry:

```properties
quarkus.tls."my-cert".trust-store.pem.certs=certs/ca.crt
ark.client."user-api".tls-configuration-name=my-cert
```

For Vert.x Mutiny transports, the extension uses `VertxTlsResolver` which converts TLS Registry entries to native Vert.x `TrustOptions`/`KeyCertOptions` - no SSLContext conversion needed.

---

## Declarative Clients

With `@RegisterArkClient`, proxy beans are auto-created and injected via CDI:

```java
@RegisterArkClient(configKey = "user-api", baseUrl = "${api.users.url:https://fallback.com}")
@Path("/users")
@Produces("application/json")
public interface UserApi {
    @GET @Path("/{id}")
    Uni<User> findById(@PathParam("id") Long id);
}
```

```java
@ApplicationScoped
public class UserService {
    @Inject UserApi userApi;
}
```

See [Declarative JAX-RS Clients](declarative-jaxrs.md) for full details.

---

## RawResponse as a return type

Proxy methods can declare `RawResponse` (or `Uni<RawResponse>`) as the
return type. This bypasses deserialization and auto-disables
throw-on-error for that method — useful for inspecting error bodies that
don't match a typed schema or for non-JSON responses.

```java
@RegisterArkClient(configKey = "users-api")
@Path("/users")
public interface UserApi {
    @GET @Path("/{id}")
    Uni<User> getUser(@PathParam("id") String id);            // type-safe, fails Uni on 4xx/5xx

    @GET @Path("/{id}")
    Uni<RawResponse> getUserRaw(@PathParam("id") String id);  // raw, never fails the Uni
}
```

The raw method auto-disables throw-on-error for its requests — no need to
set `throw-on-error=false` at the client level just for this method.

---

## Native Image

Supports GraalVM native image out of the box. The extension auto-discovers `@RegisterArkClient` interfaces at build time and registers JDK proxy definitions.

---

## Related

- [Getting Started](getting-started.md)
- [Quarkus Integration](quarkus.md)
- [Declarative JAX-RS Clients](declarative-jaxrs.md)
- [Mutiny Client](mutiny.md)
- [Testing](testing.md)