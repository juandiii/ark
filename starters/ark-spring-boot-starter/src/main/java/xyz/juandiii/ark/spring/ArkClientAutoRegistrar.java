package xyz.juandiii.ark.spring;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Auto-discovers @RegisterArkClient interfaces using Spring Boot's auto-configuration packages.
 * Activated automatically by ArkAutoConfiguration.
 *
 * @author Juan Diego Lopez V.
 */
public class ArkClientAutoRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

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
                iface -> !ArkClientRegistrations.usesReactorReturnTypes(iface), ArkClientFactoryBean.class,
                "arkClients");
    }
}
