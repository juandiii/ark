package xyz.juandiii.ark.async.proxy;

import xyz.juandiii.ark.async.AsyncArk;
import xyz.juandiii.ark.core.proxy.ArkProxy;
import xyz.juandiii.ark.core.proxy.RequestDispatcher;
import xyz.juandiii.ark.core.proxy.ReturnTypeHandler;

/**
 * Provides async dispatcher and return type handler for ArkProxy auto-detection.
 *
 * @author Juan Diego Lopez V.
 */
public final class AsyncExecutionModelProvider implements ArkProxy.ExecutionModelAccess {

    @Override
    public RequestDispatcher dispatcher(Object arkClient) {
        return AsyncDispatchers.async((AsyncArk) arkClient);
    }

    @Override
    public ReturnTypeHandler returnTypeHandler() {
        return new AsyncReturnTypeHandler();
    }
}