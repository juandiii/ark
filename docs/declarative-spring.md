# Declarative Spring Clients

Ark supports declarative HTTP clients using Spring's `@HttpExchange` annotations. Define an interface, annotate the methods, and Ark generates the implementation.

---

## Example

```java
@RegisterArkClient(baseUrl = "${api.users.url}")
@HttpExchange("/users")
public interface UserApi {

    @GetExchange("/{id}")
    User getUser(@PathVariable String id);

    @GetExchange
    List<User> listUsers(@RequestParam int page, @RequestParam int size);

    @PostExchange
    User createUser(@RequestBody CreateUserRequest request);

    @DeleteExchange("/{id}")
    void deleteUser(@PathVariable String id);
}
```

> `@RegisterArkClient` auto-creates the proxy bean. Supports property placeholders: `${property.key}` or `${property.key:default}`.

## @RegisterArkClient Attributes

| Attribute | Default | Description |
|-----------|---------|-------------|
| `configKey` | `""` | Key for per-client config in `application.properties` |
| `baseUrl` | `""` | Base URL, supports `${property}` placeholders |
| `httpVersion` | `HTTP_2` | HTTP/1.1 or HTTP/2 |
| `connectTimeout` | `10` | Connection timeout (seconds) |
| `readTimeout` | `30` | Read timeout (seconds) |
| `interceptors` | `{}` | Interceptor classes (auto-detects `RequestInterceptor` / `ResponseInterceptor`) |

## Per-Client Configuration

Use `configKey` to configure clients via `application.properties`:

```java
@RegisterArkClient(configKey = "user-api")
@HttpExchange("/users")
public interface UserApi { ... }
```

```properties
# Spring Boot
ark.client.user-api.base-url=https://api.example.com
ark.client.user-api.http-version=HTTP_2
ark.client.user-api.connect-timeout=5
ark.client.user-api.read-timeout=15
ark.client.user-api.tls-configuration-name=my-cert
ark.client.user-api.trust-all=true
ark.client.user-api.headers.X-Api-Key=${API_KEY}

# TLS bundle
spring.ssl.bundle.pem.my-cert.truststore.certificate=classpath:certs/ca.crt
```

Properties take precedence over annotation values. If no `configKey` is set, the fully qualified interface name is used.

---

## Creating the Client

### Automatic (recommended)

With `@RegisterArkClient`, the bean is auto-created — just inject it:

```java
@RestController
public class UserController {

    private final UserApi userApi;

    public UserController(UserApi userApi) {
        this.userApi = userApi;
    }
}
```

```properties
# application.properties
api.users.url=https://api.example.com
```

### Manual

```java
Ark ark = ArkClient.builder()
    .serializer(new JacksonSerializer(new ObjectMapper()))
    .transport(new ArkJdkHttpTransport(HttpClient.newBuilder().build()))
    .baseUrl("https://api.example.com")
    .build();

UserApi userApi = ArkProxy.create(UserApi.class, ark);
```

---

## Supported Annotations

| Annotation | Usage |
|------------|-------|
| `@HttpExchange` | Base path on the interface |
| `@GetExchange` | GET method |
| `@PostExchange` | POST method |
| `@PutExchange` | PUT method |
| `@PatchExchange` | PATCH method |
| `@DeleteExchange` | DELETE method |
| `@PathVariable` | URI template variable |
| `@RequestParam` | Query parameter |
| `@RequestHeader` | HTTP header |
| `@RequestBody` | Request body (serialized) |

Both `value` and `url` work: `@GetExchange("/users")` and `@GetExchange(url = "/users")`.

---

## Return Types

| Return Type | Behavior |
|-------------|----------|
| `T` | Deserializes response body |
| `void` | Calls `toBodilessEntity()` |
| `ArkResponse<T>` | Full response (status + headers + body) |
| `String` | Raw response body |
| `Mono<T>` | Reactor reactive (requires `ark-spring-boot-starter-webflux`) |
| `Mono<ArkResponse<T>>` | Reactor full response |
| `Flux<T>` | Reactor stream from JSON array |

---

## Reactive Client (Reactor)

Pass a `ReactorArk` client to `ArkProxy.create()` for reactive Spring WebFlux clients:

```java
ReactorArk reactorArk = ReactorArkClient.builder()
    .serializer(new JacksonSerializer(new ObjectMapper()))
    .transport(new ArkReactorNettyTransport(HttpClient.create()))
    .baseUrl("https://api.example.com")
    .build();

UserReactiveApi api = ArkProxy.create(UserReactiveApi.class, reactorArk);
```

```java
@HttpExchange("/users")
public interface UserReactiveApi {

    @GetExchange("/{id}")
    Mono<User> getUser(@PathVariable String id);

    @GetExchange
    Flux<User> listUsers();

    @PostExchange
    Mono<User> createUser(@RequestBody User user);

    @DeleteExchange("/{id}")
    Mono<Void> deleteUser(@PathVariable String id);
}
```

---

## Request Mapping

```java
@HttpExchange("/users")
public interface UserApi {

    @PostExchange("/{id}/roles")
    void assignRole(
        @PathVariable String id,
        @RequestParam String source,
        @RequestHeader("X-Request-Id") String requestId,
        @RequestBody RoleRequest request
    );
}
```

Produces:

```http
POST /users/123/roles?source=admin
X-Request-Id: 8b7d...
Content-Type: application/json

{"role": "admin"}
```

---

## Form Data

### MultiValueMap

```java
@PostExchange("/token")
Token authenticate(@RequestBody MultiValueMap<String, String> form);
```

Automatically sets `Content-Type: application/x-www-form-urlencoded` and encodes as `key=value&key2=value2`.

### Map with explicit content type

```java
@PostExchange(contentType = "application/x-www-form-urlencoded")
Token authenticate(@RequestBody Map<String, String> form);
```

---

## Sub-Resources

Interfaces can reference other `@HttpExchange` interfaces as sub-resources:

```java
@HttpExchange("/api")
public interface ApiClient {

    @GetExchange("/health")
    String health();

    UserApi users();
    OrderApi orders();
}

@HttpExchange("/users")
public interface UserApi {

    @GetExchange("/{id}")
    User findById(@PathVariable String id);
}

@HttpExchange("/orders")
public interface OrderApi {

    @GetExchange("/{id}")
    Order findById(@PathVariable String id);
}
```

```java
ApiClient api = ArkProxy.create(ApiClient.class, ark);
api.health();                    // GET /api/health
api.users().findById("123");     // GET /api/users/123
api.orders().findById("456");    // GET /api/orders/456
```

Paths concatenate automatically: parent `@HttpExchange` + child `@HttpExchange`.

---

## Interface Inheritance

Methods from parent interfaces are inherited with their own `@HttpExchange` base path:

```java
@HttpExchange("/api")
public interface BaseClient {

    @GetExchange("/health")
    String health();
}

@HttpExchange("/users")
public interface UserClient extends BaseClient {

    @GetExchange("/{id}")
    User findById(@PathVariable String id);
}
```

```java
UserClient client = ArkProxy.create(UserClient.class, ark);
client.health();            // GET /api/health (from BaseClient)
client.findById("123");     // GET /users/123 (from UserClient)
```

---

## Interceptors

Register per-client interceptors via the annotation:

```java
@RegisterArkClient(configKey = "user-api", interceptors = {AuthInterceptor.class})
@HttpExchange("/users")
public interface UserApi { ... }
```

```java
@Component
public class AuthInterceptor implements RequestInterceptor {
    private final TokenService tokenService;

    public AuthInterceptor(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public void intercept(RequestContext ctx) {
        ctx.header("Authorization", "Bearer " + tokenService.getToken());
    }
}
```

A class implementing both `RequestInterceptor` and `ResponseInterceptor` is auto-detected and registered for both.

---

## Logging

See [Logging](logging.md) for full details. Quick setup:

```properties
ark.logging.level=BODY
```

---

## Error Handling

Same typed exceptions as fluent clients:

```java
try {
    User user = userApi.getUser("123");
} catch (NotFoundException e) {
    // 404
} catch (UnauthorizedException e) {
    // 401
} catch (TimeoutException e) {
    // request timed out
}
```

See [Sync Client — Error Handling](sync.md#error-handling) for the full exception hierarchy.

---

## GraalVM Native Image

Annotate interfaces with `@RegisterArkClient` — the `ark-spring-boot-starter` auto-registers them as JDK proxy definitions at AOT build time:

```java
@RegisterArkClient(baseUrl = "${api.users.url}")
@HttpExchange("/users")
public interface UserApi {
    @GetExchange("/{id}")
    User getUser(@PathVariable String id);
}
```

No additional configuration needed. The starter auto-discovers `@RegisterArkClient` interfaces at build time.

---

## When to Use

**Declarative** when:
- Spring ecosystem
- Interface-first design
- Less boilerplate
- Contract-oriented API

**Fluent** when:
- Dynamic request composition
- Explicit control
- Programmatic request building

Both styles coexist in the same project.

---

## Related

- [Getting Started](getting-started.md)
- [Spring Boot Integration](spring-boot.md)
- [Testing](testing.md)