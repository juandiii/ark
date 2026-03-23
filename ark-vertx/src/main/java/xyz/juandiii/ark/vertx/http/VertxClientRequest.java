package xyz.juandiii.ark.vertx.http;

import xyz.juandiii.ark.interceptor.RequestContext;

import java.time.Duration;

public interface VertxClientRequest extends RequestContext {

    VertxClientRequest accept(String mediaType);

    VertxClientRequest contentType(String mediaType);

    VertxClientRequest header(String key, String value);

    VertxClientRequest queryParam(String key, String value);

    VertxClientRequest body(Object body);

    VertxClientRequest timeout(Duration timeout);

    VertxClientResponse retrieve();
}
