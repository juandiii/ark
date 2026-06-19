package xyz.juandiii.ark.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MimeTypeTest {

    @Test
    void givenJpegExtension_thenReturnsJpeg() {
        assertEquals(MimeType.JPEG, MimeType.fromExtension(".jpg"));
        assertEquals(MimeType.JPEG, MimeType.fromExtension(".jpeg"));
    }

    @Test
    void givenPngExtension_thenReturnsPng() {
        assertEquals(MimeType.PNG, MimeType.fromExtension(".png"));
    }

    @Test
    void givenUpperCase_thenStillMatches() {
        assertEquals(MimeType.PNG, MimeType.fromExtension(".PNG"));
    }

    @Test
    void givenUnknown_thenReturnsOctetStream() {
        assertEquals(MimeType.OCTET_STREAM, MimeType.fromExtension(".abc"));
    }

    @Test
    void givenValue_thenReturnsMimeString() {
        assertEquals("image/jpeg", MimeType.JPEG.value());
        assertEquals("application/json", MimeType.JSON.value());
        assertEquals("application/octet-stream", MimeType.OCTET_STREAM.value());
    }

    @Test
    void givenToString_thenReturnsMimeString() {
        assertEquals("image/png", MimeType.PNG.toString());
    }

    @Test
    void givenExtensions_thenReturnsArray() {
        assertTrue(MimeType.JPEG.extensions().length >= 2);
        assertTrue(MimeType.GZIP.extensions().length >= 2);
    }

    @Test
    void givenYamlExtensions_thenBothMatch() {
        assertEquals(MimeType.YAML, MimeType.fromExtension(".yaml"));
        assertEquals(MimeType.YAML, MimeType.fromExtension(".yml"));
    }

    @Test
    void givenHtmlExtensions_thenBothMatch() {
        assertEquals(MimeType.HTML, MimeType.fromExtension(".html"));
        assertEquals(MimeType.HTML, MimeType.fromExtension(".htm"));
    }
}
