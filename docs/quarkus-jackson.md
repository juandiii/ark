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

## Native Image

Supports GraalVM native image out of the box. No additional configuration needed.

---

## Related

- [Getting Started](getting-started.md)
- [Quarkus Integration](quarkus.md)
- [Mutiny Client](mutiny.md)
- [Testing](testing.md)