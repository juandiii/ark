# Error Handling

Ark provides typed exceptions for HTTP errors and transport failures. Catch specific exception types instead of inspecting status codes.

---

## Exception Hierarchy

```
ArkException (transport/IO errors)
  +-- TimeoutException
  +-- ConnectionException
  +-- RequestInterruptedException

ApiException (HTTP status >= 400)
  +-- ClientException (4xx)
  |     +-- BadRequestException (400)
  |     +-- UnauthorizedException (401)
  |     +-- ForbiddenException (403)
  |     +-- NotFoundException (404)
  |     +-- ConflictException (409)
  |     +-- UnprocessableEntityException (422)
  |     +-- TooManyRequestsException (429)
  +-- ServerException (5xx)
        +-- InternalServerErrorException (500)
        +-- BadGatewayException (502)
        +-- ServiceUnavailableException (503)
        +-- GatewayTimeoutException (504)
```

Unmapped status codes throw `ClientException` (4xx) or `ServerException` (5xx) with the raw status code.

---

## Sync

```java
try {
    User user = client.get("/users/1")
        .retrieve()
        .body(User.class);
} catch (NotFoundException e) {
    // 404
} catch (UnauthorizedException e) {
    // 401
} catch (ClientException e) {
    // other 4xx
} catch (ServerException e) {
    // 5xx
} catch (TimeoutException e) {
    // request timed out
} catch (ConnectionException e) {
    // connection refused, DNS failure
}
```

---

## Async

Errors are wrapped in `CompletionException`:

```java
client.get("/users/1")
    .retrieve()
    .body(User.class)
    .exceptionally(ex -> {
        if (ex.getCause() instanceof NotFoundException) {
            return User.unknown();
        }
        throw new CompletionException(ex);
    });
```

---

## Reactor

```java
client.get("/users/1")
    .retrieve()
    .body(User.class)
    .onErrorResume(NotFoundException.class, ex ->
        Mono.just(User.unknown()))
    .onErrorResume(ServerException.class, ex ->
        Mono.error(new ServiceUnavailableException("Upstream error")));
```

---

## Mutiny

```java
client.get("/users/1")
    .retrieve()
    .body(User.class)
    .onFailure(NotFoundException.class).recoverWithItem(User.unknown())
    .onFailure(ServerException.class).retry()
        .withBackOff(Duration.ofMillis(500))
        .atMost(3);
```

---

## Vert.x

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

## Declarative Clients

Same exceptions apply to `@RegisterArkClient` proxy methods:

```java
try {
    User user = userApi.getUser("123");
} catch (NotFoundException e) {
    // 404
} catch (TimeoutException e) {
    // request timed out
}
```

---

## ApiException Details

All `ApiException` subclasses provide:

```java
catch (ApiException e) {
    int status = e.statusCode();       // HTTP status code
    String body = e.responseBody();    // raw response body
}
```

---

## Transport Exceptions

| Exception | When |
|-----------|------|
| `TimeoutException` | Connect or read timeout exceeded |
| `ConnectionException` | Connection refused, DNS failure, connection reset |
| `RequestInterruptedException` | Thread interrupted during request |

All extend `ArkException` and provide `method()` and `uri()` for request tracing.

---

## Retry Behavior

By default, retry only applies to:
- **Status codes:** 429, 502, 503, 504
- **Exceptions:** `TimeoutException`, `ConnectionException`
- **Methods:** GET, HEAD, PUT, DELETE, OPTIONS (idempotent only)

See [Retry & Backoff](retry.md) for configuration.

---

## Related

- [Retry & Backoff](retry.md)
- [Logging](logging.md)
- [Sync Client](sync.md)
- [Testing](testing.md)
