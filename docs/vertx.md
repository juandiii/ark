# Vert.x Client

## Building the Client

```java
VertxArk client = VertxArkClient.builder()
    .serializer(serializer)
    .transport(new ArkVertxFutureTransport(webClient))
    .baseUrl("https://api.example.com")
    .build();
```

## Making Requests

```java
Future<User> user = client.get("/users/1")
    .retrieve()
    .body(User.class);
```

## Vert.x Transports

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

## Same Fluent API

The request-building experience stays the same -- only the return type changes.

```java
Future<User> future = vertxClient.get("/users/1")
    .retrieve()
    .body(User.class);
```
