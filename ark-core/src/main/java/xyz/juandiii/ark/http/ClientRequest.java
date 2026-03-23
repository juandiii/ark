package xyz.juandiii.ark.http;

import xyz.juandiii.ark.interceptor.RequestContext;

import java.time.Duration;

public interface ClientRequest extends RequestContext {

    ClientRequest accept(String mediaType);

    ClientRequest contentType(String mediaType);

    ClientRequest header(String key, String value);

    ClientRequest queryParam(String key, String value);

    ClientRequest body(Object body);

    ClientRequest timeout(Duration timeout);

    ClientResponse retrieve();
}
