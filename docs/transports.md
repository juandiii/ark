# Transport Model

Ark uses a **bridge pattern** - the transport is a thin adapter around an already configured HTTP client. Timeouts, SSL, connection pools, and HTTP version are configured on the underlying client itself, not on Ark.

All transports implement a single generic contract `Transport<R>` where `R` is the return-type wrapper for the execution model (sync value, future, reactive publisher). Decorators compose via `transport.with(...)`.

---

## Transport Interfaces

| Interface | R (return type) | Module |
|-----------|------------------|--------|
| `HttpTransport extends Transport<RawResponse>` | `RawResponse` (sync) | `ark-core` |
| `ReactorHttpTransport extends Transport<Mono<RawResponse>>` | `Mono<RawResponse>` | `ark-reactor` |
| `MutinyHttpTransport extends Transport<Uni<RawResponse>>` | `Uni<RawResponse>` | `ark-mutiny` |
| `VertxHttpTransport extends Transport<Future<RawResponse>>` | `Future<RawResponse>` | `ark-vertx` |
| `Transport<CompletableFuture<RawResponse>>` (no marker interface) | `CompletableFuture<RawResponse>` | `ark-async` |

The async (CompletableFuture) execution model uses the generic `Transport<R>` directly with no peer interface — `ArkJdkAsyncTransport` implements `Transport<CompletableFuture<RawResponse>>` directly.

---

## Built-in Transports

| Transport | Implements | Module |
|-----------|------------|--------|
| `ArkJdkSyncTransport` | `HttpTransport` (sync) | `ark-transport-jdk` |
| `ArkJdkAsyncTransport` | `Transport<CompletableFuture<RawResponse>>` | `ark-transport-jdk` |
| `ArkReactorNettyTransport` | `ReactorHttpTransport` | `ark-transport-reactor` |
| `ArkVertxFutureTransport` | `VertxHttpTransport` | `ark-transport-vertx` |
| `ArkVertxMutinyTransport` | `MutinyHttpTransport` | `ark-transport-vertx-mutiny` |
| `ArkApacheTransport` | `HttpTransport` | `ark-transport-apache` |

> Both `ArkJdkSyncTransport` and `ArkJdkAsyncTransport` can wrap the **same** underlying Java `HttpClient` instance. Use that pattern to share the connection pool between sync and async modes.

### Java Native HttpClient

```xml
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-transport-jdk</artifactId>
</dependency>
```

```java
HttpClient httpClient = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_2)
    .connectTimeout(Duration.ofSeconds(10))
    .sslContext(sslContext)
    .executor(Executors.newVirtualThreadPerTaskExecutor())
    .build();

// Sync — for ArkClient
ArkJdkSyncTransport syncTransport = new ArkJdkSyncTransport(httpClient);

// Async — for AsyncArkClient (same HttpClient, shared pool)
ArkJdkAsyncTransport asyncTransport = new ArkJdkAsyncTransport(httpClient);
```

### Reactor Netty

```xml
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-transport-reactor</artifactId>
</dependency>
```

```java
reactor.netty.http.client.HttpClient httpClient = HttpClient.create()
    .responseTimeout(Duration.ofSeconds(30))
    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
    .secure(ssl -> ssl.sslContext(sslContext));

ArkReactorNettyTransport transport = new ArkReactorNettyTransport(httpClient);
```

### Vert.x Future

```xml
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-transport-vertx</artifactId>
</dependency>
```

```java
WebClient webClient = WebClient.create(vertx, new WebClientOptions()
    .setSsl(true)
    .setConnectTimeout(5000)
    .setMaxPoolSize(50));

ArkVertxFutureTransport transport = new ArkVertxFutureTransport(webClient);
```

### Vert.x Mutiny

```xml
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-transport-vertx-mutiny</artifactId>
</dependency>
```

```java
io.vertx.mutiny.ext.web.client.WebClient webClient = WebClient.create(vertx);

ArkVertxMutinyTransport transport = new ArkVertxMutinyTransport(webClient);
```

### Apache HttpClient 5

```xml
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-transport-apache</artifactId>
</dependency>
```

```java
CloseableHttpClient httpClient = HttpClients.custom()
    .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
        .setMaxConnTotal(100)
        .setMaxConnPerRoute(20)
        .build())
    .setDefaultRequestConfig(RequestConfig.custom()
        .setResponseTimeout(Timeout.ofSeconds(30))
        .build())
    .build();

ArkApacheTransport transport = new ArkApacheTransport(httpClient);
```

---

## Decorator chain: `transport.with(...)`

Every `Transport<R>` exposes a default `with(Function<Transport<R>, Transport<R>>)` method that composes decorators. Built-in decorator: `Retry<R>` (see [Retry & Backoff](retry.md)). Custom decorators (metrics, caching, circuit breaker) plug in the same way.

```java
import xyz.juandiii.ark.core.http.decorator.Retry;
import xyz.juandiii.ark.core.http.decorator.SyncRetryOps;

HttpTransport base = new ArkJdkSyncTransport(httpClient);
Transport<RawResponse> resilient = base.with(Retry.of(policy, new SyncRetryOps()));
                                      // .with(MyMetrics.of(registry))
                                      // .with(MyCache.of(store));

Ark client = ArkClient.builder()
    .transport(resilient)
    .serializer(serializer)
    .build();
```

**Order matters** — the last `.with(...)` is the outermost wrapper. `Retry` outside `Metrics` measures per-attempt latency; `Metrics` outside `Retry` measures total wall-clock including backoff. See [Retry & Backoff → Decorator order](retry.md#decorator-order).

---

## Custom Transport

Implement either the marker sub-interface for your execution model, or `Transport<R>` directly:

```java
public class MyTransport implements HttpTransport {

    private final MyHttpClient client;

    public MyTransport(MyHttpClient client) {
        this.client = client;
    }

    @Override
    public RawResponse send(String method, URI uri, Map<String, String> headers,
                            String body, Duration timeout) {
        // Adapt your client's API and return RawResponse
    }

    // sendBinary default throws UnsupportedOperationException;
    // override when your transport handles binary uploads
    @Override
    public RawResponse sendBinary(String method, URI uri, Map<String, String> headers,
                                   byte[] body, Duration timeout) {
        // Same as send, but use the binary-safe path of your client
    }
}
```

Custom decorator:

```java
public final class MyDecorator<R> implements Transport<R> {
    private final Transport<R> delegate;

    public MyDecorator(Transport<R> delegate) { this.delegate = delegate; }

    public static <R> Function<Transport<R>, Transport<R>> of() {
        return MyDecorator::new;
    }

    @Override
    public R send(String method, URI uri, Map<String, String> headers,
                  String body, Duration timeout) {
        // pre-call work
        R result = delegate.send(method, uri, headers, body, timeout);
        // post-call work
        return result;
    }

    @Override
    public R sendBinary(String method, URI uri, Map<String, String> headers,
                        byte[] body, Duration timeout) {
        return delegate.sendBinary(method, uri, headers, body, timeout);
    }
}

// Usage:
transport.with(MyDecorator.of()).with(Retry.of(...));
```

---

## Transport Logging

Transport-level logging is handled by `LoggingInterceptor` at the framework layer (not by transports themselves). Configure with `ark.logging.level=NONE|BASIC|HEADERS|BODY` in Spring / Quarkus, or programmatically via `LoggingInterceptor.apply(builder, Level.HEADERS)`. Sensitive headers (Authorization, Cookie, X-API-Key, etc.) and common credential body keys are auto-redacted.

For raw wire-level transport debugging, enable the underlying client's own logger — see [Logging](logging.md).

---

## Related

- [Getting Started](getting-started.md)
- [Sync Client](sync.md)
- [Async Client](async.md)
- [Reactor Client](reactor.md)
- [Mutiny Client](mutiny.md)
- [Retry & Backoff](retry.md) - decorator chain with `Retry<R>` + `RetryOps<R>` per model
- [Design Principles](design.md)
