package xyz.juandiii.ark.reactor.http;

import xyz.juandiii.ark.core.interceptor.RequestContext;

import java.time.Duration;

/**
 * Interface for reactive request configuration and execution.
 *
 * @author Juan Diego Lopez V.
 */
public interface ReactorClientRequest extends RequestContext {

    ReactorClientRequest accept(String mediaType);

    ReactorClientRequest contentType(String mediaType);

    ReactorClientRequest header(String key, String value);

    ReactorClientRequest queryParam(String key, String value);

    ReactorClientRequest body(Object body);

    ReactorClientRequest timeout(Duration timeout);

    ReactorClientResponse retrieve();
}
