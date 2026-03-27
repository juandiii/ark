package xyz.juandiii.ark;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.juandiii.ark.interceptor.RequestInterceptor;
import xyz.juandiii.ark.interceptor.ResponseInterceptor;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AbstractArkBuilderTest {

    @Mock
    JsonSerializer serializer;

    @Mock
    RequestInterceptor requestInterceptor;

    @Mock
    ResponseInterceptor responseInterceptor;

    private TestBuilder builder() {
        return new TestBuilder();
    }

    @Test
    void defaultValues() {
        TestBuilder b = builder();
        assertEquals("", b.baseUrl);
        assertEquals(ArkVersion.NAME, b.name);
        assertEquals(ArkVersion.VERSION, b.version);
        assertTrue(b.requestInterceptors.isEmpty());
        assertTrue(b.responseInterceptors.isEmpty());
    }

    @Test
    void setsSerializer() {
        TestBuilder b = builder().serializer(serializer);
        assertSame(serializer, b.serializer);
    }

    @Test
    void setsBaseUrl() {
        TestBuilder b = builder().baseUrl("https://api.example.com");
        assertEquals("https://api.example.com", b.baseUrl);
    }

    @Test
    void setsUserAgent() {
        TestBuilder b = builder().userAgent("MyApp", "2.0");
        assertEquals("MyApp", b.name);
        assertEquals("2.0", b.version);
        assertEquals("MyApp/2.0", b.buildUserAgent());
    }

    @Test
    void defaultUserAgent() {
        assertEquals(ArkVersion.NAME + "/" + ArkVersion.VERSION, builder().buildUserAgent());
    }

    @Test
    void addsRequestInterceptor() {
        TestBuilder b = builder().requestInterceptor(requestInterceptor);
        assertEquals(1, b.requestInterceptors.size());
        assertSame(requestInterceptor, b.requestInterceptors.get(0));
    }

    @Test
    void addsMultipleRequestInterceptors() {
        RequestInterceptor second = ctx -> {};
        TestBuilder b = builder()
                .requestInterceptor(requestInterceptor)
                .requestInterceptor(second);
        assertEquals(2, b.requestInterceptors.size());
    }

    @Test
    void addsResponseInterceptor() {
        TestBuilder b = builder().responseInterceptor(responseInterceptor);
        assertEquals(1, b.responseInterceptors.size());
    }

    @Test
    void fluentChainingReturnsSameInstance() {
        TestBuilder b = builder();
        assertSame(b, b.serializer(serializer));
        assertSame(b, b.baseUrl("url"));
        assertSame(b, b.userAgent("app", "1.0"));
        assertSame(b, b.requestInterceptor(requestInterceptor));
        assertSame(b, b.responseInterceptor(responseInterceptor));
    }

    @Test
    void formatConfigurationIncludesAllFields() {
        TestBuilder b = builder().serializer(serializer).baseUrl("https://api.example.com");
        String result = b.formatConfiguration("TestClient", "TestTransport");

        assertTrue(result.contains("TestClient"));
        assertTrue(result.contains("TestTransport"));
        assertTrue(result.contains("https://api.example.com"));
    }

    @Test
    void formatConfigurationWithNullSerializer() {
        TestBuilder b = builder().baseUrl("https://api.example.com");
        String result = b.formatConfiguration("TestClient", "TestTransport");

        assertTrue(result.contains("(not set)"));
    }

    @Test
    void formatConfigurationWithEmptyBaseUrl() {
        TestBuilder b = builder().serializer(serializer);
        String result = b.formatConfiguration("TestClient", "TestTransport");

        assertTrue(result.contains("Base URL: (not set)"));
    }

    @Test
    void logConfigurationDoesNotThrow() {
        TestBuilder b = builder().serializer(serializer).baseUrl("https://api.example.com");
        assertDoesNotThrow(() -> b.logConfiguration("TestClient", "TestTransport"));
    }

    static class TestBuilder extends AbstractArkBuilder<TestBuilder> {}
}