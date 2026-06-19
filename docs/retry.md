# Retry & Backoff

Ark provides automatic retry with exponential backoff for transient failures. Implemented as a transport decorator - transparent to the client code.

---

## Configuration

### Spring Boot

```properties
ark.client.user-api.retry.max-attempts=3
ark.client.user-api.retry.delay=500
ark.client.user-api.retry.multiplier=2.0
ark.client.user-api.retry.max-delay=30000
ark.client.user-api.retry.retry-on=429,502,503,504
ark.client.user-api.retry.retry-on-exception=true
ark.client.user-api.retry.retry-post=false
```

### Quarkus

```properties
ark.client."user-api".retry.max-attempts=3
ark.client."user-api".retry.delay=500
ark.client."user-api".retry.multiplier=2.0
ark.client."user-api".retry.max-delay=30000
ark.client."user-api".retry.retry-on=429,502,503,504
ark.client."user-api".retry.retry-on-exception=true
ark.client."user-api".retry.retry-post=false
```

If `max-attempts` is 0 or 1 (default), retry is disabled - no overhead.

---

## Properties

| Property | Default | Description |
|----------|---------|-------------|
| `max-attempts` | `0` (disabled) | Maximum number of attempts (including the first) |
| `delay` | `500` | Initial delay in milliseconds |
| `multiplier` | `2.0` | Backoff multiplier applied after each attempt |
| `max-delay` | `30000` | Maximum delay cap in milliseconds |
| `retry-on` | `429,502,503,504` | HTTP status codes that trigger a retry |
| `retry-on-exception` | `true` | Retry on transport errors (timeout, connection) |
| `retry-post` | `false` | Retry non-idempotent methods (POST, PATCH) |

---

## How It Works

### Exponential Backoff with Jitter

Each retry waits longer than the previous one. Jitter randomizes the delay to avoid thundering herd.

```
Attempt 1: fail → wait [250ms, 500ms]
Attempt 2: fail → wait [500ms, 1000ms]
Attempt 3: fail → give up, throw exception
```

Formula: `delay * multiplier^(attempt-1)`, capped at `max-delay`, then randomized between 50% and 100% of the computed value.

### What Gets Retried

**By status code** (configurable via `retry-on`):
- `429` - Too Many Requests (rate limiting)
- `502` - Bad Gateway
- `503` - Service Unavailable
- `504` - Gateway Timeout

**By exception** (when `retry-on-exception=true`):
- `TimeoutException` - request timed out
- `ConnectionException` - connection refused, DNS failure

**Never retried:**
- `400`, `401`, `403`, `404`, `409`, `422` - client errors
- `RequestInterruptedException` - thread interrupted

### Method Safety

Only idempotent methods are retried by default: `GET`, `HEAD`, `PUT`, `DELETE`, `OPTIONS`.

`POST` and `PATCH` are **not retried** unless `retry-post=true`. Enable this only if your API is idempotent for these methods.

---

## Log Output

```
WARN  Retry 1/3 for GET https://api.example.com/users (HTTP 503) - waiting 398ms
WARN  Retry 2/3 for GET https://api.example.com/users (HTTP 503) - waiting 872ms
ERROR Retry exhausted 3/3 for GET https://api.example.com/users (HTTP 503) - giving up
```

Logger name: `xyz.juandiii.ark.retry`

---

## Programmatic Usage

For standalone use without Spring/Quarkus. Compose retry via the
`transport.with(Retry.of(policy, ops))` decorator chain. The `RetryOps<R>`
strategy is per execution model:

| Module | Class | Strategy `RetryOps<R>` |
|---|---|---|
| `ark-core` | `Retry<RawResponse>` (sync) | `SyncRetryOps` |
| `ark-async` | `Retry<CompletableFuture<RawResponse>>` | `AsyncRetryOps` |
| `ark-reactor` | `Retry<Mono<RawResponse>>` | `ReactorRetryOps` |
| `ark-mutiny` | `Retry<Uni<RawResponse>>` | `MutinyRetryOps` |
| `ark-vertx` | `Retry<Future<RawResponse>>` | `VertxRetryOps` |

### Sync

```java
import xyz.juandiii.ark.core.http.decorator.Retry;
import xyz.juandiii.ark.core.http.decorator.SyncRetryOps;

RetryPolicy policy = RetryPolicy.builder()
    .maxAttempts(3)
    .delay(Duration.ofMillis(500))
    .multiplier(2.0)
    .retryOn(Set.of(429, 503))
    .build();

Transport<RawResponse> transport = new ArkJdkSyncTransport(HttpClient.newBuilder().build())
    .with(Retry.of(policy, new SyncRetryOps()));

Ark client = ArkClient.builder()
    .serializer(serializer)
    .transport(transport)
    .baseUrl("https://api.example.com")
    .build();
```

### Async

```java
import xyz.juandiii.ark.async.http.decorator.AsyncRetryOps;
import xyz.juandiii.ark.core.http.decorator.Retry;

Transport<CompletableFuture<RawResponse>> transport =
    new ArkJdkAsyncTransport(HttpClient.newBuilder().build())
        .with(Retry.of(RetryPolicy.defaults(), new AsyncRetryOps()));

AsyncArk client = AsyncArkClient.builder()
    .serializer(serializer)
    .transport(transport)
    .baseUrl("https://api.example.com")
    .build();
```

### Decorator order

`with(...)` returns a new wrapping `Transport<R>`. Chains compose
**outside-in** — the last `.with(...)` is the outermost.

```java
transport
    .with(Retry.of(policy, new SyncRetryOps()))   // wrap 1
    .with(MyMetrics.of(registry));                 // wrap 2 — outermost
```

For sync, this means `MyMetrics.send()` runs first, calls into
`Retry.send()`, which calls into the underlying transport. Metrics that
need to measure total wall-clock (including backoff sleeps) go
**outside** retry; metrics that need per-attempt latency go **inside**.

---

## Reactive (Reactor / Mutiny / Vert.x)

Both options work:

1. **Ark's `Retry<R>` decorator** — uniform RetryPolicy + status filter across all execution models. Use `ReactorRetryOps`, `MutinyRetryOps`, or `VertxRetryOps` as the strategy.
2. **Native operators** of the ecosystem (`Mono.retryWhen`, `Uni.onFailure().retry()`) — fully supported, you can mix.

**Reactor (Ark's decorator):**
```java
import xyz.juandiii.ark.reactor.http.decorator.ReactorRetryOps;

Transport<Mono<RawResponse>> transport = new ArkReactorNettyTransport(httpClient)
    .with(Retry.of(policy, new ReactorRetryOps()));
```

**Reactor (native operator alternative):**

**Reactor:**
```java
client.get("/users/1")
    .retrieve()
    .body(User.class)
    .retryWhen(Retry.backoff(3, Duration.ofMillis(500))
        .filter(ex -> ex instanceof ServerException));
```

**Mutiny:**
```java
client.get("/users/1")
    .retrieve()
    .body(User.class)
    .onFailure(ServerException.class)
        .retry()
        .withBackOff(Duration.ofMillis(500))
        .atMost(3);
```

---

## Related

- [Logging](logging.md) - request/response logging
- [Getting Started](getting-started.md)
- [Sync Client](sync.md) - error handling, exception hierarchy
- [Transport Model](transports.md)