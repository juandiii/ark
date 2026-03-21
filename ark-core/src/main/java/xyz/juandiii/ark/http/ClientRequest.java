package xyz.juandiii.ark.http;

import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.interceptor.RequestContext;
import xyz.juandiii.ark.interceptor.RequestInterceptor;
import xyz.juandiii.ark.interceptor.ResponseInterceptor;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public final class ClientRequest implements RequestContext {

    private final String method;
    private final String baseUrl;
    private final String path;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private final Map<String, String> queryParams = new LinkedHashMap<>();
    private Object body;
    private Duration timeout;
    private final HttpTransport transport;
    private final JsonSerializer serializer;
    private final List<RequestInterceptor> requestInterceptors;
    private final List<ResponseInterceptor> responseInterceptors;

    public ClientRequest(String method, String baseUrl, String path,
                         HttpTransport transport, JsonSerializer serializer,
                         List<RequestInterceptor> requestInterceptors,
                         List<ResponseInterceptor> responseInterceptors) {
        this.method = method;
        this.baseUrl = baseUrl;
        this.path = path;
        this.transport = transport;
        this.serializer = serializer;
        this.requestInterceptors = requestInterceptors;
        this.responseInterceptors = responseInterceptors;
    }

    @Override
    public ClientRequest accept(String mediaType) {
        headers.put("Accept", mediaType);
        return this;
    }

    @Override
    public ClientRequest contentType(String mediaType) {
        headers.put("Content-Type", mediaType);
        return this;
    }

    @Override
    public ClientRequest header(String key, String value) {
        headers.put(key, value);
        return this;
    }

    @Override
    public ClientRequest queryParam(String key, String value) {
        if (value != null) {
            queryParams.put(key, value);
        }
        return this;
    }

    @Override
    public ClientRequest body(Object body) {
        this.body = body;
        return this;
    }

    @Override
    public ClientRequest timeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    public ResponseSpec retrieve() {
        requestInterceptors.forEach(interceptor -> interceptor.intercept(this));
        String serializedBody = needsBody() ? serializer.serialize(body) : null;
        RawResponse raw = transport.send(method, buildUri(), headers, serializedBody, timeout);
        for (ResponseInterceptor interceptor : responseInterceptors) {
            raw = interceptor.intercept(raw);
        }
        return new ResponseSpec(raw, serializer);
    }

    private boolean needsBody() {
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method);
    }

    private URI buildUri() {
        String url = baseUrl + path;
        if (!queryParams.isEmpty()) {
            StringJoiner joiner = new StringJoiner("&");
            queryParams.forEach((k, v) -> joiner.add(encode(k) + "=" + encode(v)));
            url += "?" + joiner;
        }
        return URI.create(url);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
