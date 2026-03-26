package xyz.juandiii.ark.proxy;

import xyz.juandiii.ark.TypeRef;
import xyz.juandiii.ark.http.ArkResponse;
import xyz.juandiii.ark.http.ClientRequest;
import xyz.juandiii.ark.interceptor.RequestContext;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Dispatches sync request execution based on method return type.
 *
 * @author Juan Diego Lopez V.
 */
public final class SyncReturnTypeHandler implements ReturnTypeHandler {

    @Override
    public Object handle(RequestContext request, Type returnType) {
        ClientRequest syncRequest = (ClientRequest) request;

        if (returnType == void.class || returnType == Void.class) {
            syncRequest.retrieve().toBodilessEntity();
            return null;
        }

        if (returnType instanceof ParameterizedType pt
                && pt.getRawType() == ArkResponse.class) {
            Type bodyType = pt.getActualTypeArguments()[0];
            return syncRequest.retrieve().toEntity(TypeRef.of(bodyType));
        }

        return syncRequest.retrieve().body(TypeRef.of(returnType));
    }
}
