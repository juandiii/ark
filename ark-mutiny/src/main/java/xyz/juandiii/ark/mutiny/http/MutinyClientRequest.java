package xyz.juandiii.ark.mutiny.http;

import xyz.juandiii.ark.interceptor.RequestContext;

import java.time.Duration;

public interface MutinyClientRequest extends RequestContext {

    MutinyClientRequest accept(String mediaType);

    MutinyClientRequest contentType(String mediaType);

    MutinyClientRequest header(String key, String value);

    MutinyClientRequest queryParam(String key, String value);

    MutinyClientRequest body(Object body);

    MutinyClientRequest timeout(Duration timeout);

    MutinyClientResponse retrieve();
}
