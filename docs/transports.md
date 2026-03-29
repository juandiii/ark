# Transport Model

Ark uses a **bridge pattern** — the transport is a thin adapter around an already configured HTTP client. Timeouts, SSL, connection pools, and HTTP version are configured on the client itself, not on Ark.

---

## Transport Interfaces

| Interface | Returns | Module |
|-----------|---------|--------|
| `HttpTransport` | `RawResponse` | `ark-core` |
| `AsyncHttpTransport` | `CompletableFuture<RawResponse>` | `ark-async` |
| `ReactorHttpTransport` | `Mono<RawResponse>` | `ark-reactor` |
| `MutinyHttpTransport` | `Uni<RawResponse>` | `ark-mutiny` |
| `VertxHttpTransport` | `Future<RawResponse>` | `ark-vertx` |

A single transport can implement multiple interfaces. For example, `ArkJdkHttpTransport` implements both `HttpTransport` and `AsyncHttpTransport`.

---

## Built-in Transports

| Transport | Implements | Module |
|-----------|------------|--------|
| `ArkJdkHttpTransport` | `HttpTransport` + `AsyncHttpTransport` | `ark-transport-jdk` |
| `ArkReactorNettyTransport` | `ReactorHttpTransport` | `ark-transport-reactor` |
| `ArkVertxFutureTransport` | `VertxHttpTransport` | `ark-transport-vertx` |
| `ArkVertxMutinyTransport` | `MutinyHttpTransport` | `ark-transport-vertx-mutiny` |
| `ArkApacheTransport` | `HttpTransport` | `ark-transport-apache` |

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

ArkJdkHttpTransport transport = new ArkJdkHttpTransport(httpClient);
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

## Custom Transport

Implement the interface that matches your execution model:

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
}
```

---

## Transport Logging

All built-in transports include DEBUG-level logging via `TransportLogger`, showing method, URL, scheme, host, port, path, query, headers, body, and status. Enable with your logging framework:

```properties
# application.properties (Quarkus)
quarkus.log.category."xyz.juandiii.ark.core.http.TransportLogger".level=DEBUG

# application.properties (Spring Boot)
logging.level.xyz.juandiii.ark.core.http.TransportLogger=DEBUG
```

For application-level logging (paired request/response with timing), use `LoggingInterceptor` instead — see [Sync Client](sync.md#logging).

---

## Related

- [Getting Started](getting-started.md)
- [Sync Client](sync.md)
- [Reactor Client](reactor.md)
- [Mutiny Client](mutiny.md)
- [Design Principles](design.md)