package xyz.juandiii.ark.proxy.jaxrs;

import xyz.juandiii.ark.core.Ark;
import xyz.juandiii.ark.async.AsyncArk;
import xyz.juandiii.ark.async.proxy.AsyncDispatchers;
import xyz.juandiii.ark.async.proxy.AsyncReturnTypeHandler;
import xyz.juandiii.ark.mutiny.MutinyArk;
import xyz.juandiii.ark.mutiny.proxy.MutinyDispatchers;
import xyz.juandiii.ark.mutiny.proxy.MutinyReturnTypeHandler;
import xyz.juandiii.ark.core.proxy.ArkProxy;
import xyz.juandiii.ark.core.proxy.Dispatchers;
import xyz.juandiii.ark.core.proxy.RequestDispatcher;
import xyz.juandiii.ark.core.proxy.ReturnTypeHandler;
import xyz.juandiii.ark.core.proxy.SyncReturnTypeHandler;

/**
 * Dynamic proxy factory for creating HTTP clients from JAX-RS annotated interfaces.
 *
 * @author Juan Diego Lopez V.
 */
public final class ArkJaxRsProxy {

    private ArkJaxRsProxy() {}

    public static <T> T create(Class<T> clientInterface, Ark ark) {
        return create(clientInterface, Dispatchers.sync(ark), new SyncReturnTypeHandler());
    }

    public static <T> T create(Class<T> clientInterface, AsyncArk ark) {
        return create(clientInterface, AsyncDispatchers.async(ark), new AsyncReturnTypeHandler());
    }

    public static <T> T create(Class<T> clientInterface, MutinyArk ark) {
        return create(clientInterface, MutinyDispatchers.mutiny(ark), new MutinyReturnTypeHandler());
    }

    public static <T> T create(Class<T> clientInterface,
                               RequestDispatcher dispatcher,
                               ReturnTypeHandler returnTypeHandler) {
        return ArkProxy.create(clientInterface, dispatcher,
                new JaxRsAnnotationResolver(),
                new JaxRsParameterBinder(),
                returnTypeHandler);
    }
}