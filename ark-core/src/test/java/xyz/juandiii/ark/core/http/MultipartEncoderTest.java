package xyz.juandiii.ark.core.http;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class MultipartEncoderTest {

    @Test
    void givenFieldPart_thenEncodesCorrectly() {
        MultipartBody body = MultipartBody.builder("test-boundary")
                .field("name", "Juan")
                .build();

        byte[] encoded = MultipartEncoder.encode(body);
        String result = new String(encoded, StandardCharsets.UTF_8);

        assertTrue(result.contains("--test-boundary\r\n"));
        assertTrue(result.contains("Content-Disposition: form-data; name=\"name\""));
        assertTrue(result.contains("\r\n\r\nJuan\r\n"));
        assertTrue(result.endsWith("--test-boundary--\r\n"));
    }

    @Test
    void givenFilePart_thenEncodesWithFilename() {
        MultipartBody body = MultipartBody.builder("test-boundary")
                .file("avatar", "hello".getBytes(), "photo.png", "image/png")
                .build();

        byte[] encoded = MultipartEncoder.encode(body);
        String result = new String(encoded, StandardCharsets.UTF_8);

        assertTrue(result.contains("Content-Disposition: form-data; name=\"avatar\"; filename=\"photo.png\""));
        assertTrue(result.contains("Content-Type: image/png"));
        assertTrue(result.contains("hello"));
    }

    @Test
    void givenMultipleParts_thenAllEncoded() {
        MultipartBody body = MultipartBody.builder("boundary123")
                .file("doc", "content".getBytes(), "file.txt", "text/plain")
                .field("description", "A document")
                .build();

        byte[] encoded = MultipartEncoder.encode(body);
        String result = new String(encoded, StandardCharsets.UTF_8);

        // Two boundary separators + closing boundary
        assertEquals(3, result.split("--boundary123").length - 1);
        assertTrue(result.contains("name=\"doc\""));
        assertTrue(result.contains("name=\"description\""));
    }

    @Test
    void givenBinaryContent_thenPreservedInOutput() {
        byte[] binary = new byte[]{0, 1, 2, (byte) 255, (byte) 128};
        MultipartBody body = MultipartBody.builder("b")
                .file("data", binary, "data.bin", "application/octet-stream")
                .build();

        byte[] encoded = MultipartEncoder.encode(body);

        // Verify binary bytes are present in the output
        String header = "Content-Type: application/octet-stream\r\n\r\n";
        int headerIndex = new String(encoded, StandardCharsets.ISO_8859_1).indexOf(header);
        assertTrue(headerIndex > 0);

        int contentStart = headerIndex + header.length();
        for (int i = 0; i < binary.length; i++) {
            assertEquals(binary[i], encoded[contentStart + i]);
        }
    }

    @Test
    void givenContentType_thenIncludesBoundary() {
        MultipartBody body = MultipartBody.builder("my-boundary").build();

        assertEquals("multipart/form-data; boundary=my-boundary",
                MultipartEncoder.contentType(body));
    }

    @Test
    void givenEmptyBody_thenOnlyClosingBoundary() {
        MultipartBody body = MultipartBody.builder("b").build();
        byte[] encoded = MultipartEncoder.encode(body);
        String result = new String(encoded, StandardCharsets.UTF_8);

        assertEquals("--b--\r\n", result);
    }
}
