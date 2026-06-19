package xyz.juandiii.ark.core.util;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ContentTypeDetectorTest {

    @Nested
    class MagicBytes {

        @Test
        void givenJpegBytes_thenDetectsJpeg() {
            byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
            assertEquals("image/jpeg", ContentTypeDetector.detect(jpeg, "unknown"));
        }

        @Test
        void givenPngBytes_thenDetectsPng() {
            byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
            assertEquals("image/png", ContentTypeDetector.detect(png, "unknown"));
        }

        @Test
        void givenGifBytes_thenDetectsGif() {
            byte[] gif = {0x47, 0x49, 0x46, 0x38, 0x39, 0x61};
            assertEquals("image/gif", ContentTypeDetector.detect(gif, "unknown"));
        }

        @Test
        void givenPdfBytes_thenDetectsPdf() {
            byte[] pdf = {0x25, 0x50, 0x44, 0x46, 0x2D};
            assertEquals("application/pdf", ContentTypeDetector.detect(pdf, "unknown"));
        }

        @Test
        void givenZipBytes_thenDetectsZip() {
            byte[] zip = {0x50, 0x4B, 0x03, 0x04};
            assertEquals("application/zip", ContentTypeDetector.detect(zip, "unknown"));
        }

        @Test
        void givenGzipBytes_thenDetectsGzip() {
            byte[] gzip = {0x1F, (byte) 0x8B, 0x08};
            assertEquals("application/gzip", ContentTypeDetector.detect(gzip, "unknown"));
        }

        @Test
        void givenXmlBytes_thenDetectsXml() {
            byte[] xml = "<?xml version=\"1.0\"?>".getBytes();
            assertEquals("application/xml", ContentTypeDetector.detect(xml, "unknown"));
        }

        @Test
        void givenJsonObjectBytes_thenDetectsJson() {
            byte[] json = "{\"key\":\"value\"}".getBytes();
            assertEquals("application/json", ContentTypeDetector.detect(json, "unknown"));
        }

        @Test
        void givenJsonArrayBytes_thenDetectsJson() {
            byte[] json = "[1,2,3]".getBytes();
            assertEquals("application/json", ContentTypeDetector.detect(json, "unknown"));
        }

        @Test
        void givenWebpBytes_thenDetectsWebp() {
            byte[] webp = {0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50};
            assertEquals("image/webp", ContentTypeDetector.detect(webp, "unknown"));
        }

        @Test
        void givenUnknownBytes_thenFallsToExtension() {
            byte[] unknown = {0x00, 0x01, 0x02};
            assertEquals("text/plain", ContentTypeDetector.detect(unknown, "file.txt"));
        }

        @Test
        void givenNullContent_thenFallsToExtension() {
            assertEquals("image/png", ContentTypeDetector.detect((byte[]) null, "photo.png"));
        }

        @Test
        void givenTooShortContent_thenFallsToExtension() {
            assertEquals("application/pdf", ContentTypeDetector.detect(new byte[]{0x01}, "doc.pdf"));
        }
    }

    @Nested
    class Extension {

        @Test
        void givenJpgExtension_thenDetectsJpeg() {
            assertEquals("image/jpeg", ContentTypeDetector.detectByExtension("photo.jpg"));
        }

        @Test
        void givenJpegExtension_thenDetectsJpeg() {
            assertEquals("image/jpeg", ContentTypeDetector.detectByExtension("photo.jpeg"));
        }

        @Test
        void givenPngExtension_thenDetectsPng() {
            assertEquals("image/png", ContentTypeDetector.detectByExtension("image.png"));
        }

        @Test
        void givenPdfExtension_thenDetectsPdf() {
            assertEquals("application/pdf", ContentTypeDetector.detectByExtension("doc.pdf"));
        }

        @Test
        void givenJsonExtension_thenDetectsJson() {
            assertEquals("application/json", ContentTypeDetector.detectByExtension("data.json"));
        }

        @Test
        void givenTxtExtension_thenDetectsPlainText() {
            assertEquals("text/plain", ContentTypeDetector.detectByExtension("readme.txt"));
        }

        @Test
        void givenCsvExtension_thenDetectsCsv() {
            assertEquals("text/csv", ContentTypeDetector.detectByExtension("data.csv"));
        }

        @Test
        void givenHtmlExtension_thenDetectsHtml() {
            assertEquals("text/html", ContentTypeDetector.detectByExtension("index.html"));
        }

        @Test
        void givenUnknownExtension_thenReturnsOctetStream() {
            assertEquals("application/octet-stream", ContentTypeDetector.detectByExtension("file.xyz"));
        }

        @Test
        void givenNoExtension_thenReturnsOctetStream() {
            assertEquals("application/octet-stream", ContentTypeDetector.detectByExtension("Makefile"));
        }

        @Test
        void givenNull_thenReturnsOctetStream() {
            assertEquals("application/octet-stream", ContentTypeDetector.detectByExtension(null));
        }
    }

    @Nested
    class FilePath {

        @Test
        void givenPngFile_thenDetectsByMagicBytes(@TempDir Path tempDir) throws IOException {
            Path file = tempDir.resolve("image.png");
            Files.write(file, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x00});
            assertEquals("image/png", ContentTypeDetector.detect(file));
        }

        @Test
        void givenTextFile_thenFallsToExtension(@TempDir Path tempDir) throws IOException {
            Path file = tempDir.resolve("readme.txt");
            Files.writeString(file, "hello world");
            assertEquals("text/plain", ContentTypeDetector.detect(file));
        }
    }
}
