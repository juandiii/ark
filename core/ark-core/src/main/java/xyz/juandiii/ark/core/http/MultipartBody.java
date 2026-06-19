package xyz.juandiii.ark.core.http;

import xyz.juandiii.ark.core.exceptions.ArkException;
import xyz.juandiii.ark.core.util.ContentTypeDetector;
import xyz.juandiii.ark.core.util.MimeType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Builder for {@code multipart/form-data} request bodies (RFC 7578). Append
 * file parts ({@link #file}) and field parts ({@link #field}); the encoded
 * representation is produced by {@link MultipartEncoder}.
 *
 * <p>Each instance generates a random boundary string at construction. Parts
 * are accumulated in insertion order. The builder doubles as the final body
 * type — {@link #build()} returns {@code this}.
 *
 * @author Juan Diego Lopez V.
 */
public final class MultipartBody {

    private final List<Part> parts = new ArrayList<>();
    private final String boundary;

    private MultipartBody(String boundary) {
        this.boundary = boundary;
    }

    /**
     * @return a fresh builder with a random boundary
     */
    public static MultipartBody builder() {
        return new MultipartBody(UUID.randomUUID().toString());
    }

    static MultipartBody builder(String boundary) {
        return new MultipartBody(boundary);
    }

    /**
     * Add a file part read from disk. Content type is supplied explicitly.
     *
     * @param name        form field name
     * @param file        path to the file to upload
     * @param contentType IANA media type for the file
     * @return this builder for chaining
     * @throws ArkException if the file cannot be read
     */
    public MultipartBody file(String name, Path file, String contentType) {
        try {
            byte[] content = Files.readAllBytes(file);
            String filename = file.getFileName().toString();
            parts.add(new FilePart(name, content, filename, contentType));
            return this;
        } catch (IOException e) {
            throw new ArkException("Failed to read file: " + file, e);
        }
    }

    /**
     * Add a file part from in-memory bytes.
     *
     * @param name        form field name
     * @param content     file bytes (sent byte-for-byte)
     * @param filename    declared filename in {@code Content-Disposition}
     * @param contentType IANA media type
     * @return this builder for chaining
     */
    public MultipartBody file(String name, byte[] content, String filename, String contentType) {
        parts.add(new FilePart(name, content, filename, contentType));
        return this;
    }

    /**
     * Add a text field part.
     *
     * @param name  form field name
     * @param value field value
     * @return this builder for chaining
     */
    public MultipartBody field(String name, String value) {
        parts.add(new FieldPart(name, value));
        return this;
    }

    /**
     * Terminate the builder. Returns {@code this} for symmetry with other builders.
     *
     * @return this {@code MultipartBody}
     */
    public MultipartBody build() {
        return this;
    }

    /** @return the boundary token used in {@code Content-Type} and between parts */
    public String boundary() {
        return boundary;
    }

    /** @return an unmodifiable view of the accumulated parts in insertion order */
    public List<Part> parts() {
        return Collections.unmodifiableList(parts);
    }

    /**
     * Add a part from a value whose type is decided at runtime: {@link Path} is
     * treated as a file with detected content type; {@code byte[]} as a file with
     * an auto-detected content type and synthetic filename; anything else as a
     * text field via {@code String.valueOf(value)}.
     *
     * @param name  form field name
     * @param value file path, byte array, or any toString-able value
     * @return this builder for chaining
     * @throws ArkException if {@code value} is a path that cannot be read
     */
    public MultipartBody addPart(String name, Object value) {
        if (value instanceof Path path) {
            return file(name, path, ContentTypeDetector.detect(path));
        } else if (value instanceof byte[] bytes) {
            String contentType = ContentTypeDetector.detect(bytes, name);
            String filename = name + extensionFor(contentType);
            return file(name, bytes, filename, contentType);
        }
        return field(name, String.valueOf(value));
    }

    private static String extensionFor(String contentType) {
        for (MimeType mt : MimeType.values()) {
            if (mt.value().equals(contentType) && mt.extensions().length > 0) {
                return mt.extensions()[0];
            }
        }
        return "";
    }

    /** Sealed marker for the two supported part kinds. */
    public sealed interface Part permits FilePart, FieldPart {
        /** @return the form field name of this part */
        String name();
    }

    /**
     * A multipart file part.
     *
     * @param name        form field name
     * @param content     file bytes
     * @param filename    declared filename in {@code Content-Disposition}
     * @param contentType IANA media type
     */
    public record FilePart(String name, byte[] content, String filename, String contentType) implements Part {}

    /**
     * A multipart text field part.
     *
     * @param name  form field name
     * @param value field value
     */
    public record FieldPart(String name, String value) implements Part {}
}
