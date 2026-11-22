package xyz.juandiii.ark.spring.webflux;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import xyz.juandiii.ark.spring.ArkClientRegistrations;

/**
 * Auto-discovers @RegisterArkClient interfaces for reactive proxy creation.
 *
 * @author Juan Diego Lopez V.
 */
public class ArkWebFluxClientAutoRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                        BeanDefinitionRegistry registry) {
        ArkClientRegistrations.register(registry, environment,
                ArkClientRegistrations.autoConfigurationPackages(registry),
                ArkClientRegistrations::usesReactorReturnTypes, ArkWebFluxClientFactoryBean.class,
                "jsonSerializer", "reactorHttpTransport", "environment", "arkTlsResolver",
                "ark-" + ArkWebFluxProperties.class.getName());
    }
}
