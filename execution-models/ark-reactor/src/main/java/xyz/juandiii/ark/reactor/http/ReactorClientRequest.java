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

    /**
     * Opt out of throwing {@link xyz.juandiii.ark.core.exceptions.ApiException}
     * on HTTP error status codes (4xx/5xx). When called, the response is
     * returned to the caller unchanged regardless of status.
     *
     * @return this request for chaining
     */
    ReactorClientRequest noThrow();

    ReactorClientResponse retrieve();
}
