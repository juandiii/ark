package xyz.juandiii.ark.async.proxy;

import xyz.juandiii.ark.TypeRef;
import xyz.juandiii.ark.async.http.AsyncClientRequest;
import xyz.juandiii.ark.async.http.AsyncClientResponse;
import xyz.juandiii.ark.http.ArkResponse;
import xyz.juandiii.ark.interceptor.RequestContext;
import xyz.juandiii.ark.proxy.ReturnTypeHandler;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;

/**
 * Dispatches async request execution based on method return type.
 * Supports CompletableFuture&lt;T&gt;, CompletableFuture&lt;ArkResponse&lt;T&gt;&gt;, and void.
 *
 * @author Juan Diego Lopez V.
 */
public final class AsyncReturnTypeHandler implements ReturnTypeHandler {

    @Override
    public Object handle(RequestContext request, Type returnType) {
        AsyncClientRequest asyncRequest = (AsyncClientRequest) request;
        AsyncClientResponse response = asyncRequest.retrieve();

        if (returnType == void.class || returnType == Void.class) {
            return response.toBodilessEntity();
        }

        if (returnType instanceof ParameterizedType pt
                && pt.getRawType() == CompletableFuture.class) {
            Type innerType = pt.getActualTypeArguments()[0];
            return handleFutureType(response, innerType);
        }

        return response.body(TypeRef.of(returnType));
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
