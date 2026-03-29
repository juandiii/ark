package xyz.juandiii.ark.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.juandiii.ark.core.http.DefaultClientRequest;
import xyz.juandiii.ark.core.http.HttpTransport;
import xyz.juandiii.ark.core.interceptor.RequestInterceptor;
import xyz.juandiii.ark.core.interceptor.ResponseInterceptor;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AbstractArkClientTest {

    @Mock
    JsonSerializer serializer;

    @Mock
    HttpTransport transport;

    private TestArkClient client() {
        return new TestArkClient(transport, serializer, "TestAgent/1.0",
                "https://api.example.com", Collections.emptyList(), Collections.emptyList());
    }

    @Test
    void getFiveMethods() {
        TestArkClient c = client();
        assertNotNull(c.get("/path"));
        assertNotNull(c.post("/path"));
        assertNotNull(c.put("/path"));
        assertNotNull(c.patch("/path"));
        assertNotNull(c.delete("/path"));
    }

    @Test
    void createRequestCalledForEachMethod() {
        TestArkClient c = client();
        c.get("/a");
        c.post("/b");
        c.put("/c");
        c.patch("/d");
        c.delete("/e");
        assertEquals(5, c.createRequestCount);
    }

    @Test
    void fieldsAreStoredCorrectly() {
        TestArkClient c = client();
        assertEquals("https://api.example.com", c.baseUrl);
        assertEquals("TestAgent/1.0", c.userAgent);
        assertSame(serializer, c.serializer);
    }

    static class TestArkClient extends AbstractArkClient<DefaultClientRequest> {

        private final HttpTransport transport;
        int createRequestCount = 0;

        TestArkClient(HttpTransport transport, JsonSerializer serializer, String userAgent,
                      String baseUrl, List<RequestInterceptor> requestInterceptors,
                      List<ResponseInterceptor> responseInterceptors) {
            super(serializer, userAgent, baseUrl, requestInterceptors, responseInterceptors);
            this.transport = transport;
        }

        @Override
        protected DefaultClientRequest createRequest(String method, String path) {
            createRequestCount++;
            return new DefaultClientRequest(method, baseUrl, path, transport, serializer,
                    requestInterceptors, responseInterceptors);
        }
    }
}