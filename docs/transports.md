# Transport Model

Ark uses a **bridge pattern**.

The transport layer is a thin adapter around an already configured HTTP client.
Ark does not own connection pools, SSL setup, HTTP version selection, or low-level tuning.

Those concerns remain where they belong: in the underlying HTTP client.

## Transport interfaces

| Interface | Returns | Module |
|---|---|---|
| `HttpTransport` | `RawResponse` | `ark-core` |
| `AsyncHttpTransport` | `CompletableFuture<RawResponse>` | `ark-async` |
| `ReactorHttpTransport` | `Mono<RawResponse>` | `ark-reactor` |
| `MutinyHttpTransport` | `Uni<RawResponse>` | `ark-mutiny` |
| `VertxHttpTransport` | `Future<RawResponse>` | `ark-vertx` |

A single transport may implement multiple interfaces.

For example, `ArkJdkHttpTransport` supports both sync and async execution.

---

## Built-in Transports

### Java native HTTP client

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

```java
reactor.netty.http.client.HttpClient httpClient = HttpClient.create()
    .responseTimeout(Duration.ofSeconds(30))
    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
    .secure(ssl -> ssl.sslContext(sslContext));

ArkReactorNettyTransport transport = new ArkReactorNettyTransport(httpClient);
```

### Vert.x with `CompletableFuture`

```java
WebClient webClient = WebClient.create(vertx, new WebClientOptions()
    .setSsl(true)
    .setConnectTimeout(5000)
    .setMaxPoolSize(50));

ArkVertxTransport transport = new ArkVertxTransport(webClient);
```

### Vert.x with native `Future`

```java
ArkVertxFutureTransport transport = new ArkVertxFutureTransport(webClient);
```

### Vert.x Mutiny

```java
io.vertx.mutiny.ext.web.client.WebClient webClient = WebClient.create(vertx);

ArkVertxMutinyTransport transport = new ArkVertxMutinyTransport(webClient);
```

### Apache HttpClient 5

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

Bringing your own transport is intentionally simple.

```java
public class MyTransport implements HttpTransport {

    private final MyHttpClient client;

    public MyTransport(MyHttpClient client) {
        this.client = client;
    }

    @Override
    public RawResponse send(
            String method,
            URI uri,
            Map<String, String> headers,
            String body,
            Duration timeout
    ) {
        // Adapt your client's API and return RawResponse
    }
}
```

This makes Ark easy to integrate with existing infrastructure and internal HTTP abstractions.
