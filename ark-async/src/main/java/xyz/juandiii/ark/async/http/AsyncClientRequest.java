package xyz.juandiii.ark.async.http;

import xyz.juandiii.ark.interceptor.RequestContext;

import java.time.Duration;

public interface AsyncClientRequest extends RequestContext {

    AsyncClientRequest accept(String mediaType);

    AsyncClientRequest contentType(String mediaType);

    AsyncClientRequest header(String key, String value);

    AsyncClientRequest queryParam(String key, String value);

    AsyncClientRequest body(Object body);

    AsyncClientRequest timeout(Duration timeout);

    AsyncClientResponse retrieve();
}
