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
import java.util.concurrent.CompletableFuture;

public final class AsyncClientRequest implements RequestContext {

    private final String method;
    private final String baseUrl;
    private final String path;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private final Map<String, String> queryParams = new LinkedHashMap<>();
    private Object body;
    private Duration timeout;
    private final AsyncHttpTransport transport;
    private final JsonSerializer serializer;
    private final List<RequestInterceptor> requestInterceptors;
    private final List<ResponseInterceptor> responseInterceptors;

    public AsyncClientRequest(String method, String baseUrl, String path,
                              AsyncHttpTransport transport, JsonSerializer serializer,
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
    public AsyncClientRequest accept(String mediaType) {
        headers.put("Accept", mediaType);
        return this;
    }

    @Override
    public AsyncClientRequest contentType(String mediaType) {
        headers.put("Content-Type", mediaType);
        return this;
    }

    @Override
    public AsyncClientRequest header(String key, String value) {
        headers.put(key, value);
        return this;
    }

    @Override
    public AsyncClientRequest queryParam(String key, String value) {
        if (value != null) {
            queryParams.put(key, value);
        }
        return this;
    }

    @Override
    public AsyncClientRequest body(Object body) {
        this.body = body;
        return this;
    }

    @Override
    public AsyncClientRequest timeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    public AsyncResponseSpec retrieve() {
        requestInterceptors.forEach(interceptor -> interceptor.intercept(this));
        String serializedBody = needsBody() ? serializer.serialize(body) : null;
        CompletableFuture<RawResponse> future = transport.sendAsync(
                method, buildUri(), headers, serializedBody, timeout);
        for (ResponseInterceptor interceptor : responseInterceptors) {
            future = future.thenApply(interceptor::intercept);
        }
        return new AsyncResponseSpec(future, serializer);
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
