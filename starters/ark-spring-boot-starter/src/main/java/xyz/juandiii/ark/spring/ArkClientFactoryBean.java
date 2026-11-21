package xyz.juandiii.ark.spring;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.FactoryBean;

import xyz.juandiii.ark.core.proxy.ArkProxy;
import xyz.juandiii.ark.core.proxy.RegisterArkClient;
import xyz.juandiii.ark.core.util.StringUtils;

/**
 * Spring FactoryBean that creates an Ark proxy client. Detects the execution
 * model from the client interface's return types: if any method returns
 * {@link CompletableFuture}, the proxy delegates to {@code AsyncArkClient};
 * otherwise to {@code ArkClient} (sync). Reactive execution models live in
 * the webflux starter. Configuration is resolved by {@link ArkClients}.
 *
 * @author Juan Diego Lopez V.
 */
public class ArkClientFactoryBean<T> implements FactoryBean<T> {

    private final Class<T> clientInterface;
    private final ArkClients arkClients;

    public ArkClientFactoryBean(Class<T> clientInterface, ArkClients arkClients) {
        this.clientInterface = clientInterface;
        this.arkClients = arkClients;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getObject() {
        RegisterArkClient annotation = clientInterface.getAnnotation(RegisterArkClient.class);
        String configKey = StringUtils.isNotEmpty(annotation.configKey())
                ? annotation.configKey()
                : clientInterface.getName();

        ArkClients.ResolvedConfig rc = arkClients.resolve(configKey, annotation);
        Object arkClient = usesAsyncReturnTypes(clientInterface)
                ? arkClients.asyncBuilder(rc).build()
                : arkClients.syncBuilder(rc).build();

        return (T) ArkProxy.create(clientInterface, arkClient);
    }

    @Override
    public Class<?> getObjectType() {
        return clientInterface;
    }

    static boolean usesAsyncReturnTypes(Class<?> iface) {
        for (Method method : iface.getMethods()) {
            if (method.getReturnType() == CompletableFuture.class) {
                return true;
            }
        }
        return false;
    }
}
