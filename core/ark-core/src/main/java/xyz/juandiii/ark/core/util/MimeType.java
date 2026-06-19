package xyz.juandiii.ark.core.util;

/**
 * Known content types with their MIME value and file extensions.
 *
 * @author Juan Diego Lopez V.
 */
public enum MimeType {

    // Images
    JPEG("image/jpeg", ".jpg", ".jpeg"),
    PNG("image/png", ".png"),
    GIF("image/gif", ".gif"),
    WEBP("image/webp", ".webp"),
    SVG("image/svg+xml", ".svg"),
    ICO("image/x-icon", ".ico"),

    // Documents
    PDF("application/pdf", ".pdf"),
    DOC("application/msword", ".doc"),
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx"),
    XLS("application/vnd.ms-excel", ".xls"),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx"),

    // Archives
    ZIP("application/zip", ".zip"),
    GZIP("application/gzip", ".gz", ".gzip"),
    TAR("application/x-tar", ".tar"),

    // Data
    JSON("application/json", ".json"),
    XML("application/xml", ".xml"),
    YAML("application/yaml", ".yaml", ".yml"),
    CSV("text/csv", ".csv"),

    // Web
    HTML("text/html", ".html", ".htm"),
    CSS("text/css", ".css"),
    JAVASCRIPT("application/javascript", ".js"),

    // Text
    PLAIN_TEXT("text/plain", ".txt"),

    // Media
    MP4("video/mp4", ".mp4"),
    MP3("audio/mpeg", ".mp3"),
    WAV("audio/wav", ".wav"),

    // Default
    OCTET_STREAM("application/octet-stream");

    private final String value;
    private final String[] extensions;

    MimeType(String value, String... extensions) {
        this.value = value;
        this.extensions = extensions;
    }

    public String value() {
        return value;
    }

    public String[] extensions() {
        return extensions;
    }

    public static MimeType fromExtension(String extension) {
        String lower = extension.toLowerCase();
        for (MimeType ct : values()) {
            for (String ext : ct.extensions) {
                if (ext.equals(lower)) return ct;
            }
        }
        return OCTET_STREAM;
    }

    @Override
    public String toString() {
        return value;
    }
}
