package xyz.juandiii.ark.mutiny.proxy;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import xyz.juandiii.ark.core.TypeRef;
import xyz.juandiii.ark.core.http.ArkResponse;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.interceptor.RequestContext;
import xyz.juandiii.ark.mutiny.http.MutinyClientRequest;
import xyz.juandiii.ark.mutiny.http.MutinyClientResponse;
import xyz.juandiii.ark.core.proxy.ReturnTypeHandler;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Dispatches Mutiny request execution based on method return type.
 * Supports Uni&lt;T&gt;, Uni&lt;ArkResponse&lt;T&gt;&gt;, Uni&lt;RawResponse&gt;,
 * Multi&lt;T&gt;, and void.
 *
 * @author Juan Diego Lopez V.
 */
public final class MutinyReturnTypeHandler implements ReturnTypeHandler {

    @Override
    public Object handle(RequestContext request, Type returnType) {
        MutinyClientRequest mutinyRequest = (MutinyClientRequest) request;

        if (returnType == void.class || returnType == Void.class) {
            return mutinyRequest.retrieve().toBodilessEntity();
        }

        if (returnType instanceof ParameterizedType pt) {
            if (pt.getRawType() == Uni.class) {
                Type innerType = pt.getActualTypeArguments()[0];
                if (innerType == RawResponse.class) {
                    return mutinyRequest.noThrow().retrieve().raw();
                }
                return handleUniType(mutinyRequest.retrieve(), innerType);
            }
            if (pt.getRawType() == Multi.class) {
                return handleMultiType(mutinyRequest.retrieve(), pt.getActualTypeArguments()[0]);
            }
        }

        return mutinyRequest.retrieve().body(TypeRef.of(returnType));
    }

    private Object handleUniType(MutinyClientResponse response, Type innerType) {
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

    private Object handleMultiType(MutinyClientResponse response, Type elementType) {
        return response.bodyAsMulti(TypeRef.of(elementType));
    }
}