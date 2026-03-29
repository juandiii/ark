# Quarkus Jackson Extension

The `ark-quarkus-jackson` extension auto-configures Ark HTTP clients for Quarkus applications using Jackson 2.x for serialization.

---

## Installation

```xml
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-quarkus-jackson</artifactId>
</dependency>
```

---

## What It Provides

The extension registers the following CDI beans:

| Bean | Scope | Description |
|------|-------|-------------|
| `JsonSerializer` | `@Singleton` | `JacksonClassicSerializer` using Quarkus-managed `ObjectMapper` |
| `HttpTransport` | `@Singleton` | `ArkJdkHttpTransport` with default `HttpClient` |
| `MutinyHttpTransport` | `@Singleton` | `ArkVertxMutinyTransport` with Quarkus-managed `Vertx` |
| `ArkClient.Builder` | `@Dependent` | Pre-configured sync builder |
| `MutinyArkClient.Builder` | `@Dependent` | Pre-configured Mutiny builder |

All beans use `@DefaultBean` — define your own to override any of them.

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

Builders are `@Dependent` scope — each injection gets a fresh instance:

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
    return new ArkJdkHttpTransport(HttpClient.newBuilder()
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
ark.client."user-api".headers.X-Api-Key=${API_KEY}
ark.client."user-api".retry.max-attempts=3
ark.client."user-api".retry.delay=500
```

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

## TLS / SSL

Configure TLS via Quarkus TLS Registry:

```properties
quarkus.tls."my-cert".trust-store.pem.certs=certs/ca.crt
ark.client."user-api".tls-configuration-name=my-cert
```

For Vert.x Mutiny transports, the extension uses `VertxTlsResolver` which converts TLS Registry entries to native Vert.x `TrustOptions`/`KeyCertOptions` — no SSLContext conversion needed.

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

## Native Image

Supports GraalVM native image out of the box. The extension auto-discovers `@RegisterArkClient` interfaces at build time and registers JDK proxy definitions.

---

## Related

- [Getting Started](getting-started.md)
- [Quarkus Integration](quarkus.md)
- [Declarative JAX-RS Clients](declarative-jaxrs.md)
- [Mutiny Client](mutiny.md)
- [Testing](testing.md)