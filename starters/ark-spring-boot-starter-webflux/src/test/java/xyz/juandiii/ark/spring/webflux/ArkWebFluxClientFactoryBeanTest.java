package xyz.juandiii.ark.spring.webflux;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import xyz.juandiii.ark.spring.webflux.clients.ClientsConfig;
import xyz.juandiii.ark.spring.webflux.clients.ReactiveApi;
import xyz.juandiii.ark.spring.webflux.clients.SyncApi;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArkWebFluxClientFactoryBeanTest {

    final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ArkWebFluxAutoConfiguration.class))
            .withUserConfiguration(ClientsConfig.class)
            .withPropertyValues("ark.client.reactive-api.base-url=http://localhost:1");

    @Test
    void registersOnlyReactiveInterfaces() {
        runner.run(ctx -> {
            assertEquals(1, ctx.getBeanNamesForType(ReactiveApi.class).length);
            assertEquals(0, ctx.getBeanNamesForType(SyncApi.class).length,
                    "non-reactive interfaces belong to the sync starter");
        });
    }

    @Test
    void appliesDefaultReadTimeoutOnSharedTransport() {
        runner.run(ctx -> {
            ClientsConfig.lastTimeout.set(null);
            ctx.getBean(ReactiveApi.class).status().block();
            assertEquals(Duration.ofSeconds(30), ClientsConfig.lastTimeout.get());
        });
    }
}
