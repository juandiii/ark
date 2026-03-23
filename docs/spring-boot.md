# Spring Boot Integration

`ark-spring-boot-starter` auto-configures:

- a `JsonSerializer` bean
- a prototype-scoped `ArkClient.Builder`

## Dependency

```xml
<dependency>
    <groupId>xyz.juandiii</groupId>
    <artifactId>ark-spring-boot-starter</artifactId>
</dependency>
```

## Example with Multiple Clients

```java
@Configuration
public class HttpClientsConfig {

    @Bean
    public Ark oauthClient(ArkClient.Builder arkBuilder) {
        return arkBuilder
            .transport(new ArkJdkHttpTransport(HttpClient.newBuilder().build()))
            .baseUrl("https://oauth.provider.com")
            .build();
    }

    @Bean
    public Ark apiClient(ArkClient.Builder arkBuilder) {
        return arkBuilder
            .transport(new ArkJdkHttpTransport(HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build()))
            .baseUrl("https://api.myservice.com")
            .requestInterceptor(req ->
                req.header("Authorization", "Bearer " + tokenService.getToken()))
            .build();
    }
}
```