# Retry & Backoff

Ark provides automatic retry with exponential backoff for transient failures. Implemented as a transport decorator — transparent to the client code.

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

If `max-attempts` is 0 or 1 (default), retry is disabled — no overhead.

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
- `429` — Too Many Requests (rate limiting)
- `502` — Bad Gateway
- `503` — Service Unavailable
- `504` — Gateway Timeout

**By exception** (when `retry-on-exception=true`):
- `TimeoutException` — request timed out
- `ConnectionException` — connection refused, DNS failure

**Never retried:**
- `400`, `401`, `403`, `404`, `409`, `422` — client errors
- `RequestInterruptedException` — thread interrupted

### Method Safety

Only idempotent methods are retried by default: `GET`, `HEAD`, `PUT`, `DELETE`, `OPTIONS`.

`POST` and `PATCH` are **not retried** unless `retry-post=true`. Enable this only if your API is idempotent for these methods.

---

## Log Output

```
WARN  Retry 1/3 for GET https://api.example.com/users (HTTP 503) — waiting 398ms
WARN  Retry 2/3 for GET https://api.example.com/users (HTTP 503) — waiting 872ms
ERROR Retry exhausted 3/3 for GET https://api.example.com/users (HTTP 503) — giving up
```

Logger name: `xyz.juandiii.ark.retry`

---

## Programmatic Usage

For standalone use without Spring/Quarkus:

```java
HttpTransport transport = new RetryTransport(
    new ArkJdkHttpTransport(HttpClient.newBuilder().build()),
    RetryPolicy.builder()
        .maxAttempts(3)
        .delay(Duration.ofMillis(500))
        .multiplier(2.0)
        .retryOn(Set.of(429, 503))
        .build()
);

Ark client = ArkClient.builder()
    .serializer(serializer)
    .transport(transport)
    .baseUrl("https://api.example.com")
    .build();
```

### Async

```java
AsyncHttpTransport transport = new RetryAsyncTransport(
    new ArkJdkHttpTransport(HttpClient.newBuilder().build()),
    RetryPolicy.defaults()
);
```

---

## Reactive (Reactor / Mutiny)

Retry is **not applied** for reactive transports — use the built-in retry operators instead:

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

- [Logging](logging.md) — request/response logging
- [Getting Started](getting-started.md)
- [Sync Client](sync.md) — error handling, exception hierarchy
- [Transport Model](transports.md)