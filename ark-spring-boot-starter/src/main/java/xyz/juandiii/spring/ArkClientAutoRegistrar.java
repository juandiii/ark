package xyz.juandiii.spring;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

import java.util.List;

/**
 * Auto-discovers @RegisterArkClient interfaces using Spring Boot's auto-configuration packages.
 * Activated automatically by ArkAutoConfiguration.
 *
 * @author Juan Diego Lopez V.
 */
public class ArkClientAutoRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private static final System.Logger LOGGER = System.getLogger(ArkClientAutoRegistrar.class.getName());
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

        var scanner = ArkClientScanner.createScanner(environment);

        for (String basePackage : basePackages) {
            for (var bd : scanner.findCandidateComponents(basePackage)) {
                try {
                    Class<?> iface = Class.forName(bd.getBeanClassName());
                    var annotation = iface.getAnnotation(xyz.juandiii.ark.proxy.RegisterArkClient.class);
                    if (annotation == null) continue;

                    if (!registry.containsBeanDefinition(ArkClientScanner.beanName(iface))) {
                        ArkClientScanner.registerProxyBean(registry, iface);
                    }
                } catch (ClassNotFoundException e) {
                    LOGGER.log(System.Logger.Level.WARNING,
                            "Failed to load @RegisterArkClient class: " + bd.getBeanClassName(), e);
                }
            }
        }
    }
}
