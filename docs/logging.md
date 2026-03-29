# Logging

Ark provides two levels of logging: **application-level** (interceptor) and **transport-level** (DEBUG).

---

## Application Logging (`LoggingInterceptor`)

Paired request/response logging with timing. Applied per-client via the builder or globally via `application.properties`.

### Levels

| Level | Logs |
|-------|------|
| `NONE` | Nothing |
| `BASIC` | Method, URL, status, duration |
| `HEADERS` | BASIC + request/response headers |
| `BODY` | HEADERS + request/response body (truncated at 1024 chars) |

### Programmatic

```java
Ark client = ArkClient.builder()
    .serializer(serializer)
    .transport(transport)
    .baseUrl("https://api.example.com")
    .build();

LoggingInterceptor.apply(builder, LoggingInterceptor.Level.BODY);
```

### Configuration

```properties
# Spring Boot
ark.logging.level=BODY

# Quarkus
ark.logging.level=BODY
```

### Output Example

```
--> GET /users/1
    Authorization: Bearer eyJ...
    Accept: application/json
<-- 200 (72ms)
    Content-Type: application/json
    {"id":1,"name":"Juan"}
```

---

## Transport Logging (`TransportLogger`)

Low-level DEBUG logging built into all 5 transports. Shows full request/response details including scheme, host, port, path, query, headers, body, and status.

### Enable

```properties
# Spring Boot
logging.level.xyz.juandiii.ark.http.TransportLogger=DEBUG

# Quarkus
quarkus.log.category."xyz.juandiii.ark.http.TransportLogger".level=DEBUG
```

### Output Example

```
--> REQUEST
    Method: GET
    URL: https://api.example.com/users/1
    Scheme: https
    Host: api.example.com
    Port: 443
    Path: /users/1
    Query: (none)
    Headers:
      User-Agent: Ark/1.0.10-SNAPSHOT
      Authorization: Bearer eyJ...
<-- RESPONSE
    Status: 200
    Headers:
      Content-Type: application/json
    Body: {"id":1,"name":"Juan"}
```

---

## When to Use Which

| Use Case | Tool |
|----------|------|
| Development debugging | `LoggingInterceptor` with `BODY` level |
| Production monitoring | `LoggingInterceptor` with `BASIC` level |
| Transport-level debugging | `TransportLogger` at `DEBUG` |
| Disable all logging | `ark.logging.level=NONE` + no DEBUG on TransportLogger |

---

## Related

- [Getting Started](getting-started.md)
- [Sync Client](sync.md)
- [Transport Model](transports.md)