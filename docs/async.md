# Async Client

## Building the Client

```java
AsyncArk client = AsyncArkClient.builder()
    .serializer(new JacksonSerializer(new ObjectMapper()))
    .transport(new ArkJdkHttpTransport(HttpClient.newBuilder().build()))
    .baseUrl("https://api.example.com")
    .build();
```

## Making Requests

```java
CompletableFuture<User> user = client.get("/users/1")
    .retrieve()
    .body(User.class);
```

## Transport

`ArkJdkHttpTransport` supports both sync and async execution by implementing both `HttpTransport` and `AsyncHttpTransport`.

```java
HttpClient httpClient = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_2)
    .connectTimeout(Duration.ofSeconds(10))
    .sslContext(sslContext)
    .executor(Executors.newVirtualThreadPerTaskExecutor())
    .build();

ArkJdkHttpTransport transport = new ArkJdkHttpTransport(httpClient);
```

## Same Fluent API

The request-building experience stays the same as the sync client -- only the return type changes.

```java
CompletableFuture<User> cf = asyncClient.get("/users/1")
    .retrieve()
    .body(User.class);
```