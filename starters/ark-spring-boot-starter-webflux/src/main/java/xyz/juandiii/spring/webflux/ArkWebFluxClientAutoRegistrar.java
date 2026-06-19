package xyz.juandiii.spring.webflux;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import xyz.juandiii.ark.core.proxy.RegisterArkClient;

import java.util.List;

/**
 * Auto-discovers @RegisterArkClient interfaces for reactive proxy creation.
 *
 * @author Juan Diego Lopez V.
 */
public class ArkWebFluxClientAutoRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private static final System.Logger LOGGER = System.getLogger(ArkWebFluxClientAutoRegistrar.class.getName());
    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                        BeanDefinitionRegistry registry) {
        List<String> basePackages;
        try {
            basePackages = AutoConfigurationPackages.get(
                    (org.springframework.beans.factory.BeanFactory) registry);
        } catch (IllegalStateException e) {
            return;
        }

        var scanner = ArkWebFluxClientScanner.createScanner(environment);

        for (String basePackage : basePackages) {
            for (var bd : scanner.findCandidateComponents(basePackage)) {
                try {
                    Class<?> iface = Class.forName(bd.getBeanClassName());
                    var annotation = iface.getAnnotation(RegisterArkClient.class);
                    if (annotation == null) continue;

                    if (!registry.containsBeanDefinition(ArkWebFluxClientScanner.beanName(iface))) {
                        ArkWebFluxClientScanner.registerProxyBean(registry, iface);
                    }
                } catch (ClassNotFoundException e) {
                    LOGGER.log(System.Logger.Level.WARNING,
                            "Failed to load @RegisterArkClient class: " + bd.getBeanClassName(), e);
                }
            }
        }
    }
}
