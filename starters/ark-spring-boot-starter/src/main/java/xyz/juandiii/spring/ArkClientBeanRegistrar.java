package xyz.juandiii.spring;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import xyz.juandiii.ark.core.proxy.RegisterArkClient;

import java.util.Map;

/**
 * Scans for @RegisterArkClient interfaces in packages specified by @EnableArkClients.
 *
 * @author Juan Diego Lopez V.
 */
public class ArkClientBeanRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private static final System.Logger LOGGER = System.getLogger(ArkClientBeanRegistrar.class.getName());
    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                        BeanDefinitionRegistry registry) {
        String[] basePackages = resolveBasePackages(importingClassMetadata);
        var scanner = ArkClientScanner.createScanner(environment);

        for (String basePackage : basePackages) {
            for (var bd : scanner.findCandidateComponents(basePackage)) {
                try {
                    Class<?> iface = Class.forName(bd.getBeanClassName());
                    var annotation = iface.getAnnotation(RegisterArkClient.class);
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

    private String[] resolveBasePackages(AnnotationMetadata metadata) {
        Map<String, Object> attrs = metadata.getAnnotationAttributes(EnableArkClients.class.getName());
        if (attrs != null) {
            String[] packages = (String[]) attrs.get("basePackages");
            if (packages != null && packages.length > 0 && !packages[0].isEmpty()) return packages;
            packages = (String[]) attrs.get("value");
            if (packages != null && packages.length > 0 && !packages[0].isEmpty()) return packages;
        }
        String className = metadata.getClassName();
        int lastDot = className.lastIndexOf('.');
        return new String[]{lastDot > 0 ? className.substring(0, lastDot) : ""};
    }
}
