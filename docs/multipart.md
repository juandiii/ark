# Multipart Upload

Ark supports `multipart/form-data` file uploads with binary fidelity across all 5 transports.

---

## Fluent API

```java
client.post("/upload")
    .body(MultipartBody.builder()
        .file("avatar", Path.of("photo.png"), "image/png")
        .field("name", "Juan")
        .build())
    .retrieve()
    .body(UploadResponse.class);
```

### File from bytes

```java
byte[] pdfContent = generatePdf();

client.post("/documents")
    .body(MultipartBody.builder()
        .file("doc", pdfContent, "report.pdf", "application/pdf")
        .field("title", "Monthly Report")
        .build())
    .retrieve()
    .body(Document.class);
```

### Multiple files

```java
client.post("/gallery")
    .body(MultipartBody.builder()
        .file("photos", Path.of("img1.jpg"), "image/jpeg")
        .file("photos", Path.of("img2.jpg"), "image/jpeg")
        .field("album", "Vacation")
        .build())
    .retrieve()
    .body(Gallery.class);
```

---

## Declarative API

Use `@RequestPart` on method parameters:

### Spring (`@HttpExchange`)

```java
@RegisterArkClient(configKey = "file-api")
@HttpExchange("/api")
public interface FileApi {

    @PostExchange("/upload")
    UploadResponse upload(@RequestPart("file") Path file, @RequestPart("name") String name);

    @PostExchange("/upload")
    UploadResponse uploadBytes(@RequestPart("data") byte[] data, @RequestPart("description") String desc);
}
```

### JAX-RS

```java
@RegisterArkClient(configKey = "file-api")
@Path("/api")
public interface FileApi {

    @POST
    @Path("/upload")
    UploadResponse upload(@RequestPart("file") Path file, @RequestPart("name") String name);
}
```

### Auto-detection

| Parameter type | Part type | Content-Type |
|---------------|-----------|-------------|
| `Path` | File part (reads bytes from disk) | Detected by magic bytes + extension |
| `byte[]` | File part | Detected by magic bytes |
| Any other type | Text field (`String.valueOf()`) | - |

---

## Content Type Detection

File content types are detected automatically by `ContentTypeDetector`:

1. **Magic bytes** (priority) - inspects first 12 bytes of the file:
   - JPEG, PNG, GIF, WEBP, PDF, ZIP, GZIP, XML, JSON

2. **Extension** (fallback) - 25+ extensions mapped via `MimeType` enum

3. **Default** - `application/octet-stream` if unrecognized

You can also specify the content type explicitly:

```java
MultipartBody.builder()
    .file("data", bytes, "custom.bin", "application/x-custom")
    .build()
```

---

## Security

- **Magic bytes detection** - does not trust file extensions alone
- **Header sanitization** - filenames and part names are sanitized to prevent MIME header injection (`"`, `\r`, `\n` escaped)

---

## Transport Support

All 5 transports support binary multipart natively via `sendBinary`:

| Transport | Implementation |
|-----------|---------------|
| JDK HttpClient | `BodyPublishers.ofByteArray()` |
| Reactor Netty | `Unpooled.wrappedBuffer()` |
| Vert.x Mutiny | `Buffer.buffer(byte[])` |
| Vert.x Future | `Buffer.buffer(byte[])` |
| Apache HttpClient 5 | `ByteArrayEntity` |

---

## Related

- [Getting Started](getting-started.md)
- [Declarative Spring Clients](declarative-spring.md)
- [Declarative JAX-RS Clients](declarative-jaxrs.md)
- [Transport Model](transports.md)
- [Serialization](serialization.md)
