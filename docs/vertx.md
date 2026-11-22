# Vert.x Client

The `VertxArk` client returns `io.vertx.core.Future<T>` - Vert.x native async without Mutiny or CompletableFuture.

---

## Vert.x 4 or Vert.x 5

`ark-vertx` and `ark-transport-vertx` work on **Vert.x 4.5.x and 5.x** with the same
jars; neither needs Mutiny. Your application chooses the Vert.x version. The
differences are in your dependencies and in how you build the `WebClient`; the Ark
code (`VertxArkClient`, `ArkVertxFutureTransport`, requests, retry) is identical.

| | Vert.x 4.5.x | Vert.x 5.x |
|---|---|---|
| Ark modules | `ark-vertx` + `ark-transport-vertx` | same |
| Vert.x version comes from | Ark's default (4.5.x), or your Vert.x 4 BOM | **your** `vertx-stack-depchain` 5.x BOM (required) |
| Connection pool size | `WebClientOptions.setMaxPoolSize(n)` | `new PoolOptions().setHttp1MaxSize(n)` passed to `WebClient.create` |
| Custom CA certificate | `setTrustOptions(new PemTrustOptions()...)` (also `setPemTrustOptions`) | `setTrustOptions(new PemTrustOptions()...)` only |
| Response decompression | `setDecompressionSupported(true)` (also `setTryUseCompression`) | `setDecompressionSupported(true)` only |

Options marked "only" exist in both versions, so code that uses them compiles on 4 and 5.

---

## Installation

### Vert.x 4

```xml
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-vertx</artifactId>
</dependency>
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-transport-vertx</artifactId>
</dependency>
```

These bring in Vert.x 4.5.x (`vertx-core`, `vertx-web-client`). To pin a different
4.5.x version, import the Vert.x BOM as shown below with a 4.5.x version.

### Vert.x 5

Same two Ark dependencies, plus the Vert.x 5 BOM so Maven picks Vert.x 5 (and the
Netty 4.2 it needs) instead of the 4.5.x Ark declares:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.vertx</groupId>
            <artifactId>vertx-stack-depchain</artifactId>
            <version>5.2.3</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>xyz.juandiii</groupId>
        <artifactId>ark-vertx</artifactId>
    </dependency>
    <dependency>
        <groupId>xyz.juandiii</groupId>
        <artifactId>ark-transport-vertx</artifactId>
    </dependency>
</dependencies>
```

Gradle: `implementation(platform("io.vertx:vertx-stack-depchain:5.2.3"))`.

Check with `mvn dependency:tree | grep vertx-core`: it must show `5.x`. A mix of
Vert.x 4 and 5 jars fails at runtime.

### Transport

- **Interface:** `VertxHttpTransport` - returns `Future<RawResponse>`
- **Implementation:** `ArkVertxFutureTransport` - backed by Vert.x `WebClient`

---

## Building the Client

### Vert.x 4

```java
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

Vertx vertx = Vertx.vertx();
WebClient webClient = WebClient.create(vertx, new WebClientOptions()
    .setConnectTimeout(5000)
    .setIdleTimeout(30)
    .setMaxPoolSize(50));

VertxArk client = VertxArkClient.builder()
    .transport(new ArkVertxFutureTransport(webClient))
    .baseUrl("https://api.example.com")
    .build();
```

### Vert.x 5

The pool size moved out of `WebClientOptions` into `PoolOptions`:

```java
import io.vertx.core.Vertx;
import io.vertx.core.http.PoolOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

Vertx vertx = Vertx.vertx();
WebClient webClient = WebClient.create(vertx,
    new WebClientOptions()
        .setConnectTimeout(5000)
        .setIdleTimeout(30),
    new PoolOptions().setHttp1MaxSize(50));

VertxArk client = VertxArkClient.builder()
    .transport(new ArkVertxFutureTransport(webClient))
    .baseUrl("https://api.example.com")
    .build();
```

### Serializer

Without `.serializer(...)` the client uses `VertxJsonSerializer`, which maps POJOs with
Vert.x's own Jackson support (`Json.encode` / `Json.decodeValue`) on both versions and
also accepts `JsonObject` / `JsonArray` bodies. To share your application's
`ObjectMapper`, pass `.serializer(new JacksonClassicSerializer(objectMapper))` instead.

### Inside a verticle

Reuse the verticle's `Vertx` so responses complete on its event loop, and create the
client once in `start`:

```java
public class UserVerticle extends AbstractVerticle {

    private VertxArk users;

    @Override
    public void start() {
        WebClient webClient = WebClient.create(vertx, new WebClientOptions().setConnectTimeout(5000));
        users = VertxArkClient.builder()
            .transport(new ArkVertxFutureTransport(webClient))
            .baseUrl("https://users.example.com")
            .build();
    }
}
```

This compiles on both versions. (Vert.x 5 also offers `VerticleBase` with
`Future<?> start()`; build the client the same way there.)

### Shutdown

Ark does not own the `WebClient` or `Vertx`. Close them yourself when you are done:
`webClient.close()` and `vertx.close()`, which returns a `Future` on both versions.

---

## TLS

Use `setTrustOptions`, which works on both versions:

```java
WebClientOptions options = new WebClientOptions()
    .setSsl(true)
    .setTrustOptions(new PemTrustOptions().addCertPath("certs/ca.pem"));
```

For HTTP/2, add `.setProtocolVersion(HttpVersion.HTTP_2).setUseAlpn(true)` (both versions).

---

## Retry

Wrap the transport with Ark's `Retry` decorator and `VertxRetryOps`; the builder accepts
the wrapped transport directly. Works the same on Vert.x 4 and 5:

```java
import xyz.juandiii.ark.core.http.RetryPolicy;
import xyz.juandiii.ark.core.http.decorator.Retry;
import xyz.juandiii.ark.vertx.http.decorator.VertxRetryOps;

RetryPolicy policy = RetryPolicy.builder()
    .maxAttempts(3)
    .delay(Duration.ofMillis(200))
    .retryOn(Set.of(429, 503))
    .build();

VertxArk client = VertxArkClient.builder()
    .transport(new ArkVertxFutureTransport(webClient).with(Retry.of(policy, new VertxRetryOps())))
    .baseUrl("https://api.example.com")
    .build();
```

See [Retry](retry.md) for the policy options.

---

## Migrating from Vert.x 4 to 5

1. Import `io.vertx:vertx-stack-depchain:5.x` in `dependencyManagement` (see Installation).
2. Replace `setMaxPoolSize(n)` with `new PoolOptions().setHttp1MaxSize(n)` as the third argument of `WebClient.create`.
3. Replace `setPemTrustOptions(...)` with `setTrustOptions(...)` and `setTryUseCompression(...)` with `setDecompressionSupported(...)`.
4. Ark code does not change.

---

## Making Requests

### GET

```java
Future<User> user = client.get("/users/1")
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .body(User.class);
```

### POST

```java
Future<User> created = client.post("/users")
    .contentType(MediaType.APPLICATION_JSON)
    .body(new User("Juan", "juan@example.com"))
    .retrieve()
    .body(User.class);
```

### DELETE

```java
Future<ArkResponse<Void>> response = client.delete("/users/1")
    .retrieve()
    .toBodilessEntity();
```

### Full Response

```java
Future<ArkResponse<User>> response = client.get("/users/1")
    .retrieve()
    .toEntity(User.class);

response.onSuccess(r -> {
    int status = r.statusCode();
    User body = r.body();
});
```

---

## Vert.x Composition

Use Vert.x `Future` API for chaining:

```java
client.get("/users/1")
    .retrieve()
    .body(User.class)
    .compose(user -> saveToDb(user))
    .onSuccess(saved -> log.info("Saved: {}", saved))
    .onFailure(err -> log.error("Failed", err));
```

---

## Error Handling

See [Error Handling](error-handling.md) for the full exception hierarchy. Errors propagate through the `Future`:

```java
client.get("/users/1")
    .retrieve()
    .body(User.class)
    .onFailure(err -> {
        if (err instanceof NotFoundException) {
            // 404
        } else if (err instanceof TimeoutException) {
            // request timed out
        }
    });
```

---

## Related

- [Compatibility](compatibility.md#vertx)
- [Mutiny Client](mutiny.md)
- [Transport Model](transports.md)
- [Getting Started](getting-started.md)