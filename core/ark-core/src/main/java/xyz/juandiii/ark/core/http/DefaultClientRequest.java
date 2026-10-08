package xyz.juandiii.ark.core.http;

import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.core.exceptions.ApiException;
import xyz.juandiii.ark.core.interceptor.RequestInterceptor;
import xyz.juandiii.ark.core.interceptor.ResponseInterceptor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Default implementation of {@link ClientRequest}.
 *
 * @author Juan Diego Lopez V.
 */
public final class DefaultClientRequest extends AbstractClientRequest<DefaultClientRequest> implements ClientRequest {

    private static final int ERROR_BODY_LIMIT = 64 * 1024;

    private final Transport<RawResponse> transport;
    private final StreamingTransport streamingTransport;

    public DefaultClientRequest(String method, String baseUrl, String path,
                                Transport<RawResponse> transport, JsonSerializer serializer,
                                List<RequestInterceptor> requestInterceptors,
                                List<ResponseInterceptor> responseInterceptors) {
        this(method, baseUrl, path, transport, serializer, requestInterceptors, responseInterceptors, null);
    }

    public DefaultClientRequest(String method, String baseUrl, String path,
                                Transport<RawResponse> transport, JsonSerializer serializer,
                                List<RequestInterceptor> requestInterceptors,
                                List<ResponseInterceptor> responseInterceptors,
                                StreamingTransport streamingTransport) {
        super(method, baseUrl, path, serializer, requestInterceptors, responseInterceptors);
        this.transport = transport;
        this.streamingTransport = streamingTransport;
    }

    @Override
    public ClientResponse retrieve() {
        applyInterceptors();
        SerializedBody body = prepareBody();
        RawResponse raw = body.isBinary()
                ? transport.sendBinary(method, buildUri(), headers, body.binary(), timeout)
                : transport.send(method, buildUri(), headers, body.text(), timeout);
        for (ResponseInterceptor interceptor : responseInterceptors) {
            raw = interceptor.intercept(raw);
        }
        validateResponse(raw);
        return new DefaultClientResponse(raw, serializer);
    }

    @Override
    public StreamingResponse retrieveStream() {
        if (streamingTransport == null) throw new UnsupportedOperationException(
                "retrieveStream() requires a transport that implements StreamingTransport (e.g. ArkJdkSyncTransport)");
        applyInterceptors();
        SerializedBody body = prepareBody();
        if (body.isBinary()) throw new UnsupportedOperationException(
                "retrieveStream() does not support binary request bodies yet");
        StreamingResponse response = streamingTransport.sendStreaming(method, buildUri(), headers, body.text(), timeout);
        if (isThrowOnError() && RawResponse.isErrorStatus(response.statusCode())) {
            String text;
            try (response) {
                text = readUpTo(response.body(), ERROR_BODY_LIMIT);
            }
            throw ApiException.of(response.statusCode(), text);
        }
        return response;
    }

    private static String readUpTo(InputStream in, int max) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        try {
            while (out.size() < max) {
                int n = in.read(buf, 0, Math.min(buf.length, max - out.size()));
                if (n < 0) break;
                out.write(buf, 0, n);
            }
        } catch (IOException ignored) {
            // return what has been read so far
        }
        return out.toString(StandardCharsets.UTF_8);
    }
}
