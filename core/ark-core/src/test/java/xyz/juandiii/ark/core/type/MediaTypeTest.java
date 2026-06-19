package xyz.juandiii.ark.core.type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MediaTypeTest {

    @Test
    void applicationJson() {
        assertEquals("application/json", MediaType.APPLICATION_JSON);
    }

    @Test
    void applicationXml() {
        assertEquals("application/xml", MediaType.APPLICATION_XML);
    }

    @Test
    void textPlain() {
        assertEquals("text/plain", MediaType.TEXT_PLAIN);
    }

    @Test
    void applicationFormUrlEncoded() {
        assertEquals("application/x-www-form-urlencoded", MediaType.APPLICATION_FORM_URLENCODED);
    }

    @Test
    void multipartFormData() {
        assertEquals("multipart/form-data", MediaType.MULTIPART_FORM_DATA);
    }

    @Test
    void applicationOctetStream() {
        assertEquals("application/octet-stream", MediaType.APPLICATION_OCTET_STREAM);
    }

    @Test
    void hasPrivateConstructor() throws Exception {
        java.lang.reflect.Constructor<MediaType> constructor = MediaType.class.getDeclaredConstructor();
        assertFalse(constructor.canAccess(null));
    }
}