package xyz.juandiii.ark.proxy.spring;

import xyz.juandiii.ark.core.Ark;
import xyz.juandiii.ark.core.proxy.ArkProxy;
import xyz.juandiii.ark.core.proxy.Dispatchers;
import xyz.juandiii.ark.core.proxy.RequestDispatcher;
import xyz.juandiii.ark.core.proxy.ReturnTypeHandler;
import xyz.juandiii.ark.core.proxy.SyncReturnTypeHandler;
import xyz.juandiii.ark.reactor.ReactorArk;
import xyz.juandiii.ark.reactor.proxy.ReactorDispatchers;
import xyz.juandiii.ark.reactor.proxy.ReactorReturnTypeHandler;

/**
 * Dynamic proxy factory for creating HTTP clients from Spring @HttpExchange annotated interfaces.
 *
 * @author Juan Diego Lopez V.
 */
public final class ArkSpringProxy {

    private ArkSpringProxy() {}

    public static <T> T create(Class<T> clientInterface, Ark ark) {
        return create(clientInterface, Dispatchers.sync(ark), new SyncReturnTypeHandler());
    }

    public static <T> T create(Class<T> clientInterface, ReactorArk ark) {
        return create(clientInterface, ReactorDispatchers.reactor(ark), new ReactorReturnTypeHandler());
    }

    public static <T> T create(Class<T> clientInterface,
                               RequestDispatcher dispatcher,
                               ReturnTypeHandler returnTypeHandler) {
        return ArkProxy.create(clientInterface, dispatcher,
                new SpringAnnotationResolver(),
                new SpringParameterBinder(),
                returnTypeHandler);
    }
}