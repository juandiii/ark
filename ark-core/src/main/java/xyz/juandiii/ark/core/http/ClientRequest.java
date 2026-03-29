package xyz.juandiii.ark.core.http;

import xyz.juandiii.ark.core.interceptor.RequestContext;

import java.time.Duration;

/**
 * Interface for sync request configuration and execution.
 *
 * @author Juan Diego Lopez V.
 */
public interface ClientRequest extends RequestContext {

    ClientRequest accept(String mediaType);

    ClientRequest contentType(String mediaType);

    ClientRequest header(String key, String value);

    ClientRequest queryParam(String key, String value);

    ClientRequest body(Object body);

    ClientRequest timeout(Duration timeout);

    ClientResponse retrieve();
}
