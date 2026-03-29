# Declarative JAX-RS Clients

Ark supports declarative HTTP clients using JAX-RS annotations (`jakarta.ws.rs.*`). Define an interface, annotate the methods, and Ark generates the implementation.

---

## Example

```java
@RegisterArkClient(baseUrl = "${api.users.url}")
@Path("/users")
@Produces("application/json")
@Consumes("application/json")
public interface UserApi {

    @GET
    @Path("/{id}")
    User getUser(@PathParam("id") String id);

    @GET
    List<User> listUsers(@QueryParam("page") int page, @QueryParam("size") int size);

    @POST
    User createUser(User request);

    @DELETE
    @Path("/{id}")
    void deleteUser(@PathParam("id") String id);
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
@Path("/users")
public interface UserApi { ... }
```

```properties
# Quarkus (quoted keys)
ark.client."user-api".base-url=https://api.example.com
ark.client."user-api".http-version=HTTP_2
ark.client."user-api".connect-timeout=5
ark.client."user-api".read-timeout=15
ark.client."user-api".tls-configuration-name=my-cert
ark.client."user-api".trust-all=true
ark.client."user-api".headers.X-Api-Key=${API_KEY}
```

Properties take precedence over annotation values. If no `configKey` is set, the fully qualified interface name is used.

---

## Creating the Client

### Automatic (recommended)

With `@RegisterArkClient`, the bean is auto-created — just inject it:

```java
@ApplicationScoped
public class UserService {

    @Inject
    UserApi userApi;
}
```

```properties
# application.properties
api.users.url=https://api.example.com
```

### Manual

```java
Ark ark = ArkClient.builder()
    .serializer(serializer).transport(transport)
    .baseUrl("https://api.example.com")
    .build();

UserApi userApi = ArkProxy.create(UserApi.class, ark);
```

---

## Supported Annotations

| Annotation | Usage |
|------------|-------|
| `@Path` | Base path on interface, sub-path on method |
| `@GET` | GET method |
| `@POST` | POST method |
| `@PUT` | PUT method |
| `@PATCH` | PATCH method |
| `@DELETE` | DELETE method |
| `@PathParam` | URI template variable |
| `@QueryParam` | Query parameter |
| `@HeaderParam` | HTTP header |
| `@FormParam` | Form field (URL-encoded) |
| `@Consumes` | Request content type (class or method level) |
| `@Produces` | Response accept type (class or method level) |

No `@RequestBody` needed — unannotated parameters are treated as the request body (JAX-RS convention).

---

## Return Types

### Sync (`Ark`)

| Return Type | Behavior |
|-------------|----------|
| `T` | Deserializes response body |
| `void` | Executes request, discards response |
| `ArkResponse<T>` | Full response (status + headers + body) |

### Async (`AsyncArk`)

| Return Type | Behavior |
|-------------|----------|
| `CompletableFuture<T>` | Async deserialization |
| `CompletableFuture<ArkResponse<T>>` | Async full response |
| `CompletableFuture<Void>` | Async fire-and-forget |

### Mutiny (`MutinyArk`)

| Return Type | Behavior |
|-------------|----------|
| `Uni<T>` | Reactive single value |
| `Uni<ArkResponse<T>>` | Reactive full response |
| `Multi<T>` | Stream from JSON array |

---

## Content Negotiation

`@Consumes` and `@Produces` can be set at class level (default) or method level (override):

```java
@Path("/api")
@Consumes("application/json")
@Produces("application/json")
public interface ApiClient {

    @POST
    @Consumes("text/plain")    // overrides class-level
    @Produces("text/xml")       // overrides class-level
    String upload(String data);
}
```

---

## Form Data

Use `@FormParam` for individual form fields:

```java
@Path("/auth")
public interface AuthApi {

    @POST
    @Path("/token")
    Token authenticate(
        @FormParam("grant_type") String grantType,
        @FormParam("client_id") String clientId,
        @FormParam("client_secret") String clientSecret
    );
}
```

Automatically sets `Content-Type: application/x-www-form-urlencoded` and encodes as `key=value&key2=value2`.

---

## Implicit Body

Parameters without any annotation are treated as the request body:

```java
@Path("/users")
public interface UserApi {

    @POST
    User create(User user);  // 'user' is the body — no annotation needed
}
```

---

## Regex Path Templates

JAX-RS supports regex constraints in path templates:

```java
@GET
@Path("/{id: [0-9]+}")
User findById(@PathParam("id") Long id);
```

Ark normalizes `{id: [0-9]+}` to `{id}` before substitution.

---

## Sub-Resources

Methods without an HTTP method annotation that return a `@Path`-annotated interface are sub-resource locators:

```java
@Path("/api")
public interface ApiClient {

    @Path("/users")
    UserApi users();

    @Path("/orders")
    OrderApi orders();
}

@Path("/")
public interface UserApi {

    @GET
    @Path("/{id}")
    User findById(@PathParam("id") Long id);
}
```

```java
ApiClient api = ArkProxy.create(ApiClient.class, ark);
api.users().findById(123L);   // GET /api/users/123
```

Paths accumulate: parent `@Path` + method `@Path` + child `@Path`.

---

## Usage in Quarkus

```java
@RegisterArkClient(baseUrl = "${api.users.url}")
@Path("/users")
interface UserApi {
    @GET @Path("/{id}")
    String findById(@PathParam("id") Long id);
}

@ApplicationScoped
public class UserService {

    @Inject
    UserApi userApi;

    public String getUser(Long id) {
        return userApi.findById(id);
    }
}
```

---

## GraalVM Native Image (Quarkus)

Annotate interfaces with `@RegisterArkClient` — the `ark-quarkus-jackson` extension auto-registers them as JDK proxy definitions at build time:

```java
@RegisterArkClient(baseUrl = "${api.users.url}")
@Path("/users")
@Produces("application/json")
public interface UserApi {
    @GET
    @Path("/{id}")
    Uni<User> findById(@PathParam("id") Long id);
}
```

No additional configuration needed. The extension auto-discovers `@RegisterArkClient` interfaces at build time.

---

## Interceptors

Register per-client interceptors via the annotation:

```java
@RegisterArkClient(configKey = "user-api", interceptors = {AuthInterceptor.class})
@Path("/users")
public interface UserApi { ... }
```

```java
@ApplicationScoped
public class AuthInterceptor implements RequestInterceptor {
    @Inject TokenService tokenService;

    @Override
    public void intercept(RequestContext ctx) {
        ctx.header("Authorization", "Bearer " + tokenService.getToken());
    }
}
```

A class implementing both `RequestInterceptor` and `ResponseInterceptor` is auto-detected and registered for both.

---

## Logging

Configure request/response logging via `application.properties`:

```properties
ark.logging.level=BODY
```

Levels: `NONE`, `BASIC`, `HEADERS`, `BODY`.

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

## Related

- [Declarative Spring Clients](declarative-spring.md)
- [Getting Started](getting-started.md)
- [Quarkus Integration](quarkus.md)
- [Testing](testing.md)