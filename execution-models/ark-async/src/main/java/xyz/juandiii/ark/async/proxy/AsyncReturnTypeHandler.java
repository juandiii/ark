package xyz.juandiii.ark.async.proxy;

import xyz.juandiii.ark.core.TypeRef;
import xyz.juandiii.ark.async.http.AsyncClientRequest;
import xyz.juandiii.ark.async.http.AsyncClientResponse;
import xyz.juandiii.ark.core.http.ArkResponse;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.interceptor.RequestContext;
import xyz.juandiii.ark.core.proxy.ReturnTypeHandler;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;

/**
 * Dispatches async request execution based on method return type.
 * Supports CompletableFuture&lt;T&gt;, CompletableFuture&lt;ArkResponse&lt;T&gt;&gt;,
 * CompletableFuture&lt;RawResponse&gt;, and void.
 *
 * @author Juan Diego Lopez V.
 */
public final class AsyncReturnTypeHandler implements ReturnTypeHandler {

    @Override
    public Object handle(RequestContext request, Type returnType) {
        AsyncClientRequest asyncRequest = (AsyncClientRequest) request;

        if (returnType == void.class || returnType == Void.class) {
            return asyncRequest.retrieve().toBodilessEntity();
        }

        if (returnType instanceof ParameterizedType pt
                && pt.getRawType() == CompletableFuture.class) {
            Type innerType = pt.getActualTypeArguments()[0];

            if (innerType == RawResponse.class) {
                return asyncRequest.noThrow().retrieve().raw();
            }

            return handleFutureType(asyncRequest.retrieve(), innerType);
        }

        return asyncRequest.retrieve().body(TypeRef.of(returnType));
    }

    private Object handleFutureType(AsyncClientResponse response, Type innerType) {
        if (innerType == Void.class) {
            return response.toBodilessEntity();
        }

        if (innerType instanceof ParameterizedType innerPt
                && innerPt.getRawType() == ArkResponse.class) {
            Type bodyType = innerPt.getActualTypeArguments()[0];
            if (bodyType == Void.class) {
                return response.toBodilessEntity();
            }
            return response.toEntity(TypeRef.of(bodyType));
        }

        return response.body(TypeRef.of(innerType));
    }
}
