# Testing

Ark is easy to test because transport is an explicit dependency and all public types are interfaces.

---

## Lambda Transport

Plug in a fake transport without starting a server:

```java
HttpTransport transport = (method, uri, headers, body, timeout) ->
    new RawResponse(200,
        Map.of("Content-Type", List.of("application/json")),
        "{\"id\":1,\"name\":\"Juan\"}");

Ark client = ArkClient.builder()
    .serializer(new JacksonSerializer(new ObjectMapper()))
    .transport(transport)
    .baseUrl("https://api.example.com")
    .build();

User user = client.get("/users/1").retrieve().body(User.class);
assertEquals("Juan", user.name());
```

---

## Mocking with Mockito

`Ark`, `ClientRequest`, and `ClientResponse` are interfaces - fully mockeable:

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock Ark ark;
    @Mock ClientRequest request;
    @Mock ClientResponse response;

    @Test
    void givenUser_whenGetById_thenReturnsUser() {
        when(ark.get("/users/1")).thenReturn(request);
        when(request.retrieve()).thenReturn(response);
        when(response.body(User.class)).thenReturn(new User("Juan"));

        UserService service = new UserService(ark);
        User user = service.findById("1");

        assertEquals("Juan", user.name());
        verify(ark).get("/users/1");
    }
}
```

### Async

```java
@Mock AsyncArk asyncArk;
@Mock AsyncClientRequest asyncRequest;
@Mock AsyncClientResponse asyncResponse;

@Test
void givenUser_whenGetById_thenReturnsFuture() {
    when(asyncArk.get("/users/1")).thenReturn(asyncRequest);
    when(asyncRequest.retrieve()).thenReturn(asyncResponse);
    when(asyncResponse.body(User.class))
        .thenReturn(CompletableFuture.completedFuture(new User("Juan")));

    assertEquals("Juan", asyncArk.get("/users/1")
        .retrieve().body(User.class).join().name());
}
```

---

## Error Responses

Test error handling with typed exceptions:

```java
HttpTransport notFoundTransport = (method, uri, headers, body, timeout) ->
    new RawResponse(404, Map.of(), "Not Found");

Ark client = ArkClient.builder()
    .serializer(serializer)
    .transport(notFoundTransport)
    .baseUrl("https://api.example.com")
    .build();

// Typed exception - catches exact HTTP status
assertThrows(NotFoundException.class, () ->
    client.get("/users/999").retrieve().body(User.class));

// Server error
HttpTransport serverErrorTransport = (method, uri, headers, body, timeout) ->
    new RawResponse(503, Map.of(), "Service Unavailable");

Ark errorClient = ArkClient.builder()
    .serializer(serializer)
    .transport(serverErrorTransport)
    .baseUrl("https://api.example.com")
    .build();

ServiceUnavailableException ex = assertThrows(ServiceUnavailableException.class, () ->
    errorClient.get("/health").retrieve().body(String.class));
assertEquals(503, ex.statusCode());
```

See [Sync Client - Error Handling](sync.md#error-handling) for the full exception hierarchy.

---

## Related

- [Getting Started](getting-started.md)
- [Design Principles](design.md)