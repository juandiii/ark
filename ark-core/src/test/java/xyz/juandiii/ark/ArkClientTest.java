package xyz.juandiii.ark;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.juandiii.ark.http.ClientRequest;
import xyz.juandiii.ark.http.HttpTransport;
import xyz.juandiii.ark.http.RawResponse;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArkClientTest {

    @Mock
    JsonSerializer serializer;

    @Mock
    HttpTransport transport;

    private Ark client() {
        return ArkClient.builder()
                .serializer(serializer)
                .transport(transport)
                .baseUrl("https://api.example.com")
                .build();
    }

    @Test
    void builderRequiresSerializer() {
        assertThrows(NullPointerException.class, () ->
                ArkClient.builder().transport(transport).build());
    }

    @Test
    void builderRequiresTransport() {
        assertThrows(NullPointerException.class, () ->
                ArkClient.builder().serializer(serializer).build());
    }

    @Test
    void getReturnsClientRequest() {
        assertNotNull(client().get("/users"));
    }

    @Test
    void postReturnsClientRequest() {
        assertNotNull(client().post("/users"));
    }

    @Test
    void putReturnsClientRequest() {
        assertNotNull(client().put("/users/1"));
    }

    @Test
    void patchReturnsClientRequest() {
        assertNotNull(client().patch("/users/1"));
    }

    @Test
    void deleteReturnsClientRequest() {
        assertNotNull(client().delete("/users/1"));
    }

    @Test
    void defaultGetUsesRootPath() {
        assertNotNull(client().get());
    }

    @Test
    void defaultPostUsesRootPath() {
        assertNotNull(client().post());
    }

    @Test
    void defaultPutUsesRootPath() {
        assertNotNull(client().put());
    }

    @Test
    void defaultPatchUsesRootPath() {
        assertNotNull(client().patch());
    }

    @Test
    void defaultDeleteUsesRootPath() {
        assertNotNull(client().delete());
    }

    @Test
    void setsUserAgentHeader() {
        when(transport.send(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(new RawResponse(200, Map.of(), "{}"));

        Ark ark = ArkClient.builder()
                .serializer(serializer)
                .transport(transport)
                .userAgent("TestApp", "1.0")
                .baseUrl("https://api.example.com")
                .build();

        ark.get("/").retrieve();

        verify(transport).send(anyString(), any(), argThat(headers ->
                "TestApp/1.0".equals(headers.get("User-Agent"))), any(), any());
    }

    @Test
    void setsBaseUrl() {
        when(transport.send(anyString(), any(), anyMap(), any(), any()))
                .thenReturn(new RawResponse(200, Map.of(), "{}"));

        client().get("/users").retrieve();

        verify(transport).send(anyString(), argThat(uri ->
                uri.toString().startsWith("https://api.example.com")), anyMap(), any(), any());
    }
}