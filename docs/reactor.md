# Reactor Client

The `ReactorArk` client returns `Mono<T>` from Project Reactor — ideal for Spring WebFlux applications.

---

## Installation

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

Or use the Spring Boot WebFlux starter which auto-configures everything:

```xml
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-spring-boot-starter-webflux</artifactId>
</dependency>
```
---

## Transport

- **Implementation:** `ArkReactorNettyTransport` — backed by Reactor Netty `HttpClient`

```java
reactor.netty.http.client.HttpClient httpClient = HttpClient.create()
    .responseTimeout(Duration.ofSeconds(30))
    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
    .secure(ssl -> ssl.sslContext(sslContext))
    .compress(true);

ArkReactorNettyTransport transport = new ArkReactorNettyTransport(httpClient);
```

Timeouts, SSL, connection pooling — all configured on the Reactor Netty client, not on Ark.

---

## Building the Client

### Manual

```java
reactor.netty.http.client.HttpClient httpClient = HttpClient.create()
    .responseTimeout(Duration.ofSeconds(30))
    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);

ReactorArk client = ReactorArkClient.builder()
    .serializer(new JacksonSerializer(new ObjectMapper()))
    .transport(new ArkReactorNettyTransport(httpClient))
    .baseUrl("https://api.example.com")
    .build();
```

### With Spring Boot WebFlux Starter

```java
@Configuration
public class HttpClientsConfig {

    @Bean
    public ReactorArk apiClient(ReactorArkClient.Builder builder) {
        return builder
            .baseUrl("https://api.example.com")
            .build();
    }
}
```

---

## Making Requests

### GET

```java
Mono<User> user = client.get("/users/1")
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .body(User.class);
```

### GET with generics

```java
Mono<List<User>> users = client.get("/users")
    .queryParam("page", "1")
    .retrieve()
    .body(new TypeRef<List<User>>() {});
```

### POST

```java
Mono<User> created = client.post("/users")
    .contentType(MediaType.APPLICATION_JSON)
    .body(new User("Juan", "juan@example.com"))
    .retrieve()
    .body(User.class);
```

### DELETE

```java
Mono<ArkResponse<Void>> response = client.delete("/users/1")
    .retrieve()
    .toBodilessEntity();
```

### Full Response

```java
Mono<ArkResponse<User>> response = client.get("/users/1")
    .retrieve()
    .toEntity(User.class);

response.subscribe(r -> {
    int status = r.statusCode();
    User body = r.body();
    boolean ok = r.isSuccessful();
});
```

### Flux (streaming from JSON array)

```java
Flux<User> users = client.get("/users")
    .retrieve()
    .bodyAsFlux(User.class);
```

Deserializes the JSON array response and streams each element as a `Flux<T>`.

### Per-Request Timeout

```java
Mono<User> user = client.get("/slow-endpoint")
    .timeout(Duration.ofSeconds(60))
    .retrieve()
    .body(User.class);
```

---

## Usage in Spring WebFlux

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

    @PostMapping("/users")
    public Mono<User> createUser(@RequestBody User user) {
        return client.post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .body(user)
            .retrieve()
            .body(User.class);
    }
}
```

---

## Error Handling

Errors propagate through the `Mono`:

```java
client.get("/users/1")
    .retrieve()
    .body(User.class)
    .onErrorResume(ApiException.class, ex -> {
        if (ex.isNotFound()) {
            return Mono.just(User.unknown());
        }
        return Mono.error(ex);
    });
```

---

## Related

- [Spring Boot Integration](spring-boot.md)
- [Mutiny Client](mutiny.md)
- [Getting Started](getting-started.md)
- [Transport Model](transports.md)