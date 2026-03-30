package xyz.juandiii.ark.core.http;

import xyz.juandiii.ark.core.exceptions.ArkException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Encodes a {@link MultipartBody} into {@code byte[]} following RFC 2046 multipart/form-data format.
 *
 * @author Juan Diego Lopez V.
 */
public final class MultipartEncoder {

    private static final byte[] CRLF = "\r\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] DASHDASH = "--".getBytes(StandardCharsets.UTF_8);

    private MultipartEncoder() {}

    public static byte[] encode(MultipartBody multipart) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] boundary = multipart.boundary().getBytes(StandardCharsets.UTF_8);

            for (MultipartBody.Part part : multipart.parts()) {
                out.write(DASHDASH);
                out.write(boundary);
                out.write(CRLF);

                if (part instanceof MultipartBody.FilePart file) {
                    writeFilePart(out, file);
                } else if (part instanceof MultipartBody.FieldPart field) {
                    writeFieldPart(out, field);
                }
            }

            out.write(DASHDASH);
            out.write(boundary);
            out.write(DASHDASH);
            out.write(CRLF);

            return out.toByteArray();
        } catch (IOException e) {
            throw new ArkException("Failed to encode multipart body", e);
        }
    }

    public static String contentType(MultipartBody multipart) {
        return "multipart/form-data; boundary=" + multipart.boundary();
    }

    private static void writeFilePart(ByteArrayOutputStream out, MultipartBody.FilePart file) throws IOException {
        out.write(("Content-Disposition: form-data; name=\"" + sanitize(file.name())
                + "\"; filename=\"" + sanitize(file.filename()) + "\"").getBytes(StandardCharsets.UTF_8));
        out.write(CRLF);
        out.write(("Content-Type: " + file.contentType()).getBytes(StandardCharsets.UTF_8));
        out.write(CRLF);
        out.write(CRLF);
        out.write(file.content());
        out.write(CRLF);
    }

    private static String sanitize(String value) {
        return value.replace("\"", "\\\"").replace("\r", "").replace("\n", "");
    }

    private static void writeFieldPart(ByteArrayOutputStream out, MultipartBody.FieldPart field) throws IOException {
        out.write(("Content-Disposition: form-data; name=\"" + sanitize(field.name()) + "\"")
                .getBytes(StandardCharsets.UTF_8));
        out.write(CRLF);
        out.write(CRLF);
        out.write(field.value().getBytes(StandardCharsets.UTF_8));
        out.write(CRLF);
    }
}
