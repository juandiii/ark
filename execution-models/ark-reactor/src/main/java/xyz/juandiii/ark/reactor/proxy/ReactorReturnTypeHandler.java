package xyz.juandiii.ark.reactor.proxy;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.juandiii.ark.core.TypeRef;
import xyz.juandiii.ark.core.http.ArkResponse;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.interceptor.RequestContext;
import xyz.juandiii.ark.core.proxy.ReturnTypeHandler;
import xyz.juandiii.ark.reactor.http.ReactorClientRequest;
import xyz.juandiii.ark.reactor.http.ReactorClientResponse;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Dispatches Reactor request execution based on method return type.
 * Supports Mono&lt;T&gt;, Mono&lt;ArkResponse&lt;T&gt;&gt;, Mono&lt;RawResponse&gt;,
 * Flux&lt;T&gt;, and void.
 *
 * @author Juan Diego Lopez V.
 */
public final class ReactorReturnTypeHandler implements ReturnTypeHandler {

    @Override
    public Object handle(RequestContext request, Type returnType) {
        ReactorClientRequest reactorRequest = (ReactorClientRequest) request;

        if (returnType == void.class || returnType == Void.class) {
            return reactorRequest.retrieve().toBodilessEntity();
        }

        if (returnType instanceof ParameterizedType pt) {
            if (pt.getRawType() == Mono.class) {
                Type innerType = pt.getActualTypeArguments()[0];
                if (innerType == RawResponse.class) {
                    return reactorRequest.noThrow().retrieve().raw();
                }
                return handleMonoType(reactorRequest.retrieve(), innerType);
            }
            if (pt.getRawType() == Flux.class) {
                return handleFluxType(reactorRequest.retrieve(), pt.getActualTypeArguments()[0]);
            }
        }

        return reactorRequest.retrieve().body(TypeRef.of(returnType));
    }

    private Object handleMonoType(ReactorClientResponse response, Type innerType) {
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

    @SuppressWarnings("unchecked")
    private Object handleFluxType(ReactorClientResponse response, Type elementType) {
        return response.bodyAsFlux(TypeRef.of(elementType));
    }
}