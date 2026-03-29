package xyz.juandiii.ark.vertx.http;

import xyz.juandiii.ark.core.interceptor.RequestContext;

import java.time.Duration;

/**
 * Interface for Vert.x request configuration and execution.
 *
 * @author Juan Diego Lopez V.
 */
public interface VertxClientRequest extends RequestContext {

    VertxClientRequest accept(String mediaType);

    VertxClientRequest contentType(String mediaType);

    VertxClientRequest header(String key, String value);

    VertxClientRequest queryParam(String key, String value);

    VertxClientRequest body(Object body);

    VertxClientRequest timeout(Duration timeout);

    VertxClientResponse retrieve();
}
