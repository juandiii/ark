package xyz.juandiii.ark.reactor.proxy;

import xyz.juandiii.ark.core.proxy.ArkProxy;
import xyz.juandiii.ark.core.proxy.RequestDispatcher;
import xyz.juandiii.ark.core.proxy.ReturnTypeHandler;
import xyz.juandiii.ark.reactor.ReactorArk;

/**
 * Provides Reactor dispatcher and return type handler for ArkProxy auto-detection.
 *
 * @author Juan Diego Lopez V.
 */
public final class ReactorExecutionModelProvider implements ArkProxy.ExecutionModelAccess {

    @Override
    public RequestDispatcher dispatcher(Object arkClient) {
        return ReactorDispatchers.reactor((ReactorArk) arkClient);
    }

    @Override
    public ReturnTypeHandler returnTypeHandler() {
        return new ReactorReturnTypeHandler();
    }
}