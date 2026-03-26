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
| `Mono<T>` | Reactor reactive (requires `ReactorArk`) |
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

## Error Handling

Same model as fluent clients:

```java
try {
    User user = userApi.getUser("123");
} catch (ApiException e) {
    if (e.isNotFound()) { /* 404 */ }
} catch (ArkException e) {
    // connection or transport failure
}
```

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