package xyz.juandiii.spring;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.Map;

/**
 * Scans for @ArkClient interfaces and registers them as Spring bean definitions
 * using ArkClientFactoryBean. Compatible with Spring AOT/native compilation.
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
        String[] basePackages = resolveBasePackages(importingClassMetadata);

        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false) {
                    @Override
                    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                        return beanDefinition.getMetadata().isInterface();
                    }
                };
        scanner.addIncludeFilter(new AnnotationTypeFilter(xyz.juandiii.ark.proxy.RegisterArkClient.class));
        scanner.setEnvironment(environment);

        for (String basePackage : basePackages) {
            for (var bd : scanner.findCandidateComponents(basePackage)) {
                try {
                    Class<?> iface = Class.forName(bd.getBeanClassName());
                    xyz.juandiii.ark.proxy.RegisterArkClient annotation =
                            iface.getAnnotation(xyz.juandiii.ark.proxy.RegisterArkClient.class);
                    if (annotation == null || annotation.baseUrl().isEmpty()) continue;

                    registerProxyBean(registry, iface, annotation);
                } catch (ClassNotFoundException ignored) {
                }
            }
        }
    }

    private String[] resolveBasePackages(AnnotationMetadata metadata) {
        Map<String, Object> attrs = metadata.getAnnotationAttributes(EnableArkClients.class.getName());
        if (attrs != null) {
            String[] packages = (String[]) attrs.get("basePackages");
            if (packages != null && packages.length > 0 && !packages[0].isEmpty()) {
                return packages;
            }
            packages = (String[]) attrs.get("value");
            if (packages != null && packages.length > 0 && !packages[0].isEmpty()) {
                return packages;
            }
        }
        String className = metadata.getClassName();
        int lastDot = className.lastIndexOf('.');
        return new String[]{lastDot > 0 ? className.substring(0, lastDot) : ""};
    }

    private void registerProxyBean(BeanDefinitionRegistry registry,
                                   Class<?> iface,
                                   xyz.juandiii.ark.proxy.RegisterArkClient annotation) {

        AbstractBeanDefinition beanDef = BeanDefinitionBuilder
                .genericBeanDefinition(ArkClientFactoryBean.class)
                .addConstructorArgValue(iface)
                .addConstructorArgValue(annotation.baseUrl())
                .addConstructorArgValue(annotation.httpVersion())
                .addConstructorArgValue(annotation.connectTimeout())
                .addConstructorArgValue(annotation.readTimeout())
                .addConstructorArgReference("jsonSerializer")
                .addConstructorArgReference("environment")
                .getBeanDefinition();

        beanDef.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE,
                ResolvableType.forClass(iface));

        String beanName = Character.toLowerCase(iface.getSimpleName().charAt(0))
                + iface.getSimpleName().substring(1);
        registry.registerBeanDefinition(beanName, beanDef);
    }
}