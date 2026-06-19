package xyz.juandiii.ark.core.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MultipartBodyTest {

    @Test
    void givenField_thenPartIsAdded() {
        MultipartBody body = MultipartBody.builder()
                .field("name", "Juan")
                .build();

        assertEquals(1, body.parts().size());
        assertInstanceOf(MultipartBody.FieldPart.class, body.parts().get(0));
        assertEquals("name", body.parts().get(0).name());
        assertEquals("Juan", ((MultipartBody.FieldPart) body.parts().get(0)).value());
    }

    @Test
    void givenFileBytes_thenPartIsAdded() {
        byte[] content = "file content".getBytes();
        MultipartBody body = MultipartBody.builder()
                .file("doc", content, "report.txt", "text/plain")
                .build();

        assertEquals(1, body.parts().size());
        assertInstanceOf(MultipartBody.FilePart.class, body.parts().get(0));
        MultipartBody.FilePart part = (MultipartBody.FilePart) body.parts().get(0);
        assertEquals("doc", part.name());
        assertEquals("report.txt", part.filename());
        assertEquals("text/plain", part.contentType());
        assertArrayEquals(content, part.content());
    }

    @Test
    void givenFilePath_thenFileIsRead(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "hello world");

        MultipartBody body = MultipartBody.builder()
                .file("upload", file, "text/plain")
                .build();

        MultipartBody.FilePart part = (MultipartBody.FilePart) body.parts().get(0);
        assertEquals("test.txt", part.filename());
        assertEquals("hello world", new String(part.content()));
    }

    @Test
    void givenMultipleParts_thenAllAdded() {
        MultipartBody body = MultipartBody.builder()
                .file("avatar", new byte[]{1, 2, 3}, "photo.png", "image/png")
                .field("name", "Juan")
                .field("email", "juan@example.com")
                .build();

        assertEquals(3, body.parts().size());
    }

    @Test
    void givenBuilder_thenBoundaryIsGenerated() {
        MultipartBody body = MultipartBody.builder().build();

        assertNotNull(body.boundary());
        assertFalse(body.boundary().isEmpty());
    }

    @Test
    void givenParts_thenListIsImmutable() {
        MultipartBody body = MultipartBody.builder()
                .field("name", "Juan")
                .build();

        assertThrows(UnsupportedOperationException.class, () ->
                body.parts().add(new MultipartBody.FieldPart("x", "y")));
    }
}
