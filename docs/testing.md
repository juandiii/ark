# Testing

Ark is easy to test because transport is an explicit dependency.

You can plug in a fake or mock transport without starting a server or mocking static client code.

## Mock Transport Example

```java
HttpTransport transport = (method, uri, headers, body, timeout) ->
    new RawResponse(
        200,
        Map.of("Content-Type", List.of("application/json")),
        "{\"id\":1,\"name\":\"Juan\"}"
    );

Ark client = ArkClient.builder()
    .serializer(new JacksonSerializer(new ObjectMapper()))
    .transport(transport)
    .baseUrl("https://api.example.com")
    .build();

User user = client.get("/users/1")
    .retrieve()
    .body(User.class);
```

This makes Ark a strong fit for unit testing and library development.