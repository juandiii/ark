# Quarkus Integration

## CDI Producer Example

```java
@ApplicationScoped
public class HttpConfig {

    @Produces
    public MutinyArk apiClient(Vertx vertx, JsonSerializer serializer) {
        return MutinyArkClient.builder()
            .serializer(serializer)
            .transport(new ArkVertxMutinyTransport(WebClient.create(vertx)))
            .baseUrl("https://api.example.com")
            .build();
    }
}
```