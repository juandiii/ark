# Mutiny Client

## Building the Client

```java
MutinyArk client = MutinyArkClient.builder()
    .serializer(serializer)
    .transport(new ArkVertxMutinyTransport(webClient))
    .baseUrl("https://api.example.com")
    .build();
```

## Making Requests

```java
Uni<User> user = client.get("/users/1")
    .retrieve()
    .body(User.class);
```

## Vert.x Mutiny Transport

```java
io.vertx.mutiny.ext.web.client.WebClient webClient = WebClient.create(vertx);

ArkVertxMutinyTransport transport = new ArkVertxMutinyTransport(webClient);
```

## Same Fluent API

The request-building experience stays the same -- only the return type changes.

```java
Uni<User> uni = mutinyClient.get("/users/1")
    .retrieve()
    .body(User.class);
```