package xyz.juandiii.ark.proxy.spring;

import xyz.juandiii.ark.TypeRef;
import xyz.juandiii.ark.http.ArkResponse;
import xyz.juandiii.ark.http.ClientRequest;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

final class ReturnTypeHandler {

    Object handle(ClientRequest request, Type returnType) {
        if (returnType == void.class || returnType == Void.class) {
            // Execute the request but discard the response — void method only needs side effects
            request.retrieve().toBodilessEntity();
            return null;
        }

        if (returnType instanceof ParameterizedType pt
                && pt.getRawType() == ArkResponse.class) {
            Type bodyType = pt.getActualTypeArguments()[0];
            return request.retrieve().toEntity(TypeRef.of(bodyType));
        }

        return request.retrieve().body(TypeRef.of(returnType));
    }
}
