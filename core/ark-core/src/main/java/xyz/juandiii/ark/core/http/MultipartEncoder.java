package xyz.juandiii.ark.core.http;

import xyz.juandiii.ark.core.exceptions.ArkException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Encodes a {@link MultipartBody} into a {@code byte[]} payload following the
 * RFC 7578 / RFC 2046 {@code multipart/form-data} format. Pair with
 * {@link #contentType(MultipartBody)} to build the matching
 * {@code Content-Type} header value.
 *
 * <p>Filename and field-name strings are sanitized: all control characters
 * (0x00-0x1F, 0x7F) are stripped and double quotes are backslash-escaped,
 * so a hostile filename cannot break header framing.
 *
 * @author Juan Diego Lopez V.
 */
public final class MultipartEncoder {

    private static final byte[] CRLF = "\r\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] DASHDASH = "--".getBytes(StandardCharsets.UTF_8);
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\x00-\\x1F\\x7F]");

    private MultipartEncoder() {}

    /**
     * Encode the multipart body into its on-the-wire bytes.
     *
     * @param multipart body to encode (must not be {@code null})
     * @return encoded payload bytes
     * @throws xyz.juandiii.ark.core.exceptions.ArkException if encoding fails
     */
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

    /**
     * Build the {@code Content-Type} header value for the given body's boundary.
     *
     * @param multipart body whose boundary to embed
     * @return {@code "multipart/form-data; boundary=..."}
     */
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
        String stripped = CONTROL_CHARS.matcher(value).replaceAll("");
        return stripped.replace("\"", "\\\"");
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
