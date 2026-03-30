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
 * Builder for multipart/form-data request bodies.
 *
 * @author Juan Diego Lopez V.
 */
public final class MultipartBody {

    private final List<Part> parts = new ArrayList<>();
    private final String boundary;

    private MultipartBody(String boundary) {
        this.boundary = boundary;
    }

    public static MultipartBody builder() {
        return new MultipartBody(UUID.randomUUID().toString());
    }

    static MultipartBody builder(String boundary) {
        return new MultipartBody(boundary);
    }

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

    public MultipartBody file(String name, byte[] content, String filename, String contentType) {
        parts.add(new FilePart(name, content, filename, contentType));
        return this;
    }

    public MultipartBody field(String name, String value) {
        parts.add(new FieldPart(name, value));
        return this;
    }

    public MultipartBody build() {
        return this;
    }

    public String boundary() {
        return boundary;
    }

    public List<Part> parts() {
        return Collections.unmodifiableList(parts);
    }

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

    public sealed interface Part permits FilePart, FieldPart {
        String name();
    }

    public record FilePart(String name, byte[] content, String filename, String contentType) implements Part {}

    public record FieldPart(String name, String value) implements Part {}
}
