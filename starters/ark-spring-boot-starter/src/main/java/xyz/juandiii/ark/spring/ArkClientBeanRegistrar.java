package xyz.juandiii.ark.spring;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Scans for @RegisterArkClient interfaces in packages specified by @EnableArkClients.
 *
 * @author Juan Diego Lopez V.
 */
public class ArkClientBeanRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                        BeanDefinitionRegistry registry) {
        ArkClientRegistrations.register(registry, environment,
                ArkClientRegistrations.enabledPackages(importingClassMetadata, EnableArkClients.class),
                iface -> !ArkClientRegistrations.usesReactorReturnTypes(iface), ArkClientFactoryBean.class,
                "arkClients");
    }
}
