# Spring Boot Integration

Ark provides two starters for Spring Boot:

| Starter | Stack | Auto-configures |
|---------|-------|-----------------|
| `ark-spring-boot-starter` | Spring MVC (sync) | `JsonSerializer` + `HttpTransport` + `ArkClient.Builder` |
| `ark-spring-boot-starter-webflux` | Spring WebFlux (reactive) | `JsonSerializer` + `ReactorHttpTransport` + `ReactorArkClient.Builder` |

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

- `JsonSerializer` — `JacksonSerializer` using Spring's `ObjectMapper`
- `HttpTransport` — `ArkJdkHttpTransport` with default `HttpClient`
- `ArkClient.Builder` — prototype-scoped, pre-configured with serializer + transport

All beans use `@ConditionalOnMissingBean` — define your own to override.

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

- `JsonSerializer` — `JacksonSerializer` using Spring's `ObjectMapper`
- `ReactorHttpTransport` — `ArkReactorNettyTransport` with default Reactor Netty `HttpClient`
- `ReactorArkClient.Builder` — prototype-scoped, pre-configured with serializer + transport

### Usage

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

---

## Related

- [Getting Started](getting-started.md)
- [Sync Client](sync.md)
- [Reactor Client](reactor.md)
- [Transport Model](transports.md)
- [Testing](testing.md)