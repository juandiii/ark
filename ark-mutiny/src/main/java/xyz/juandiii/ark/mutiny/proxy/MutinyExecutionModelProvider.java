package xyz.juandiii.ark.mutiny.proxy;

import xyz.juandiii.ark.mutiny.MutinyArk;
import xyz.juandiii.ark.proxy.ArkProxy;
import xyz.juandiii.ark.proxy.RequestDispatcher;
import xyz.juandiii.ark.proxy.ReturnTypeHandler;

/**
 * Provides Mutiny dispatcher and return type handler for ArkProxy auto-detection.
 *
 * @author Juan Diego Lopez V.
 */
public final class MutinyExecutionModelProvider implements ArkProxy.ExecutionModelAccess {

    @Override
    public RequestDispatcher dispatcher(Object arkClient) {
        return MutinyDispatchers.mutiny((MutinyArk) arkClient);
    }

    @Override
    public ReturnTypeHandler returnTypeHandler() {
        return new MutinyReturnTypeHandler();
    }
}