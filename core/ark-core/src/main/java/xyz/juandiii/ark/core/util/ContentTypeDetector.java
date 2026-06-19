package xyz.juandiii.ark.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Detects content type by inspecting file magic bytes (file signature).
 * Falls back to extension-based detection if magic bytes are not recognized.
 *
 * @author Juan Diego Lopez V.
 */
public final class ContentTypeDetector {

    private ContentTypeDetector() {}

    public static String detect(Path file) {
        try {
            MimeType byMagic = detectByMagicBytes(file);
            if (byMagic != null) return byMagic.value();
        } catch (IOException ignored) {
        }
        return detectByExtension(file.getFileName().toString());
    }

    public static String detect(byte[] content, String filename) {
        MimeType byMagic = detectByMagicBytes(content);
        if (byMagic != null) return byMagic.value();
        return detectByExtension(filename);
    }

    static MimeType detectByMagicBytes(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            byte[] header = new byte[12];
            int read = in.read(header, 0, 12);
            if (read < 3) return null;
            return matchMagicBytes(header, read);
        }
    }

    static MimeType detectByMagicBytes(byte[] content) {
        if (content == null || content.length < 3) return null;
        int len = Math.min(content.length, 12);
        return matchMagicBytes(content, len);
    }

    private static MimeType matchMagicBytes(byte[] header, int length) {
        if (match(header, length, 0xFF, 0xD8, 0xFF)) return MimeType.JPEG;
        if (length >= 8 && match(header, length, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
            return MimeType.PNG;
        if (match(header, length, 0x47, 0x49, 0x46, 0x38)) return MimeType.GIF;
        if (length >= 12 && match(header, length, 0x52, 0x49, 0x46, 0x46)
                && header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50)
            return MimeType.WEBP;
        if (match(header, length, 0x25, 0x50, 0x44, 0x46)) return MimeType.PDF;
        if (match(header, length, 0x50, 0x4B, 0x03, 0x04)) return MimeType.ZIP;
        if (match(header, length, 0x1F, 0x8B)) return MimeType.GZIP;
        if (match(header, length, 0x3C, 0x3F, 0x78, 0x6D)) return MimeType.XML;
        if (header[0] == 0x7B || header[0] == 0x5B) return MimeType.JSON;
        return null;
    }

    static String detectByExtension(String filename) {
        if (filename == null) return MimeType.OCTET_STREAM.value();
        int dot = filename.lastIndexOf('.');
        if (dot < 0) return MimeType.OCTET_STREAM.value();
        return MimeType.fromExtension(filename.substring(dot)).value();
    }

    private static boolean match(byte[] header, int length, int... signature) {
        if (length < signature.length) return false;
        for (int i = 0; i < signature.length; i++) {
            if ((header[i] & 0xFF) != signature[i]) return false;
        }
        return true;
    }
}
