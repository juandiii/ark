# Reactor Client

## Building the Client

```java
ReactorArk client = ReactorArkClient.builder()
    .serializer(new JacksonSerializer(new ObjectMapper()))
    .transport(new ArkReactorNettyTransport(httpClient))
    .baseUrl("https://api.example.com")
    .build();
```

## Making Requests

```java
Mono<User> user = client.get("/users/1")
    .retrieve()
    .body(User.class);
```

## Reactor Netty Transport

```java
reactor.netty.http.client.HttpClient httpClient = HttpClient.create()
    .responseTimeout(Duration.ofSeconds(30))
    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
    .secure(ssl -> ssl.sslContext(sslContext));

ArkReactorNettyTransport transport = new ArkReactorNettyTransport(httpClient);
```

## Same Fluent API

The request-building experience stays the same -- only the return type changes.

```java
Mono<User> mono = reactorClient.get("/users/1")
    .retrieve()
    .body(User.class);
```