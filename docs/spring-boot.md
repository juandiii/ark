# Spring Boot Integration

Ark provides two starters for Spring Boot:

| Starter | Stack | Auto-configures |
|---------|-------|-----------------|
| `ark-spring-boot-starter` | Spring MVC (sync) | `JsonSerializer` + `HttpTransport` + `ArkClient.Builder` + `ArkProperties` + `TlsResolver` |
| `ark-spring-boot-starter-webflux` | Spring WebFlux (reactive) | `JsonSerializer` + `ReactorHttpTransport` + `ReactorArkClient.Builder` + `ArkWebFluxProperties` + `TlsResolver` |

Both starters support `@RegisterArkClient` declarative clients, per-client configuration, TLS/SSL, trust-all, headers, interceptors, and GraalVM native image.

---

## Spring MVC (Sync)

### Dependency

```xml
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-spring-boot-starter</artifactId>
</dependency>
```

### What It Provides

- `JsonSerializer` - `JacksonSerializer` using Spring's `ObjectMapper`
- `HttpTransport` - `ArkJdkHttpTransport` with default `HttpClient`
- `ArkClient.Builder` - prototype-scoped, pre-configured with serializer + transport

All beans use `@ConditionalOnMissingBean` - define your own to override.

### Usage

```java
@Configuration
public class HttpClientsConfig {

    @Bean
    public Ark oauthClient(ArkClient.Builder arkBuilder) {
        return arkBuilder
            .baseUrl("https://oauth.provider.com")
            .build();
    }

    @Bean
    public Ark apiClient(ArkClient.Builder arkBuilder) {
        return arkBuilder
            .baseUrl("https://api.myservice.com")
            .requestInterceptor(req ->
                req.header("Authorization", "Bearer " + tokenService.getToken()))
            .build();
    }
}
```

```java
@Service
public class UserService {

    private final Ark apiClient;

    public UserService(@Qualifier("apiClient") Ark apiClient) {
        this.apiClient = apiClient;
    }

    public User findById(String id) {
        return apiClient.get("/users/" + id)
            .retrieve()
            .body(User.class);
    }
}
```

### Custom HttpTransport

Override the default JDK transport:

```java
@Bean
public HttpTransport httpTransport() {
    return new ArkJdkHttpTransport(HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .connectTimeout(Duration.ofSeconds(10))
        .build());
}
```

---

## Spring WebFlux (Reactive)

### Dependency

```xml
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-spring-boot-starter-webflux</artifactId>
</dependency>
```

### What It Provides

- `JsonSerializer` - `JacksonSerializer` using Spring's `ObjectMapper`
- `ReactorHttpTransport` - `ArkReactorNettyTransport` with default Reactor Netty `HttpClient`
- `ReactorArkClient.Builder` - prototype-scoped, pre-configured with serializer + transport
- `ArkWebFluxProperties` - type-safe configuration (`@ConfigurationProperties`)
- `TlsResolver` - SSL bundle resolution (auto-detects `SslBundles`)
- Auto-discovery of `@RegisterArkClient` interfaces with reactive proxy creation

All beans use `@ConditionalOnMissingBean` - define your own to override.

### Fluent Usage

```java
@Configuration
public class HttpClientsConfig {

    @Bean
    public ReactorArk apiClient(ReactorArkClient.Builder arkBuilder) {
        return arkBuilder
            .baseUrl("https://api.example.com")
            .build();
    }
}
```

```java
@RestController
public class UserController {

    private final ReactorArk client;

    @GetMapping("/users/{id}")
    public Mono<User> getUser(@PathVariable String id) {
        return client.get("/users/" + id)
            .retrieve()
            .body(User.class);
    }
}
```

### Declarative Usage

```java
@RegisterArkClient(configKey = "user-api")
@HttpExchange("/users")
public interface UserApi {
    @GetExchange("/{id}")
    Mono<User> getUser(@PathVariable String id);

    @GetExchange
    Flux<User> listUsers();

    @PostExchange
    Mono<User> createUser(@RequestBody User user);
}
```

```java
@RestController
public class UserController {

    private final UserApi userApi;

    public UserController(UserApi userApi) {
        this.userApi = userApi;
    }

    @GetMapping("/users/{id}")
    public Mono<User> getUser(@PathVariable String id) {
        return userApi.getUser(id);
    }
}
```

### WebFlux Configuration

```properties
# application.properties
ark.logging.level=BODY

ark.client.user-api.base-url=https://api.example.com
ark.client.user-api.http-version=HTTP_2
ark.client.user-api.connect-timeout=5
ark.client.user-api.read-timeout=15
ark.client.user-api.trust-all=false
ark.client.user-api.headers.Authorization=Bearer ${TOKEN}
ark.client.user-api.tls-configuration-name=my-cert

# TLS bundle
spring.ssl.bundle.pem.my-cert.truststore.certificate=classpath:certs/ca.crt
```

Same configuration structure as the sync starter. See [Declarative Spring Clients](declarative-spring.md) for full annotation details.

> **Note:** Retry is not configured via properties for reactive clients - use Reactor's built-in `.retryWhen()` instead. See [Retry & Backoff](retry.md#reactive-reactor--mutiny).

### Custom @EnableArkWebFluxClients

For manual package scanning (instead of auto-discovery):

```java
@SpringBootApplication
@EnableArkWebFluxClients(basePackages = "com.example.clients")
public class MyApplication { }
```

---

## Configuration (`ArkProperties`)

The starter provides type-safe configuration via `@ConfigurationProperties`:

```properties
# application.properties

# Global logging level: NONE, BASIC, HEADERS, BODY
ark.logging.level=BODY

# Per-client configuration (key matches @RegisterArkClient configKey)
ark.client.user-api.base-url=https://api.example.com
ark.client.user-api.http-version=HTTP_2
ark.client.user-api.connect-timeout=5
ark.client.user-api.read-timeout=15
ark.client.user-api.tls-configuration-name=my-cert
ark.client.user-api.trust-all=false
ark.client.user-api.headers.X-Api-Key=${API_KEY}
ark.client.user-api.retry.max-attempts=3
ark.client.user-api.retry.delay=500
```

See [Retry & Backoff](retry.md) for full retry configuration.

```java
@RegisterArkClient(configKey = "user-api")
@HttpExchange("/users")
public interface UserApi {
    @GetExchange("/{id}")
    User getUser(@PathVariable String id);
}
```

Properties take precedence over annotation values.

---

## TLS / SSL

Configure SSL bundles in `application.properties`:

```properties
spring.ssl.bundle.pem.my-cert.truststore.certificate=classpath:certs/ca.crt
ark.client.user-api.tls-configuration-name=my-cert
```

The starter auto-detects `SslBundles` and creates a `TlsResolver` that resolves SSL contexts from Spring's SSL bundle registry.

---

## Declarative Clients

With `@RegisterArkClient`, proxy beans are auto-created:

```java
@RegisterArkClient(configKey = "user-api", baseUrl = "${api.users.url:https://fallback.com}")
@HttpExchange("/users")
public interface UserApi {
    @GetExchange("/{id}")
    User getUser(@PathVariable String id);
}
```

Just inject it:

```java
@Service
public class UserService {
    private final UserApi userApi;

    public UserService(UserApi userApi) {
        this.userApi = userApi;
    }
}
```

See [Declarative Spring Clients](declarative-spring.md) for full details.

---

## Related

- [Getting Started](getting-started.md)
- [Sync Client](sync.md)
- [Reactor Client](reactor.md)
- [Declarative Spring Clients](declarative-spring.md)
- [Transport Model](transports.md)
- [Testing](testing.md)