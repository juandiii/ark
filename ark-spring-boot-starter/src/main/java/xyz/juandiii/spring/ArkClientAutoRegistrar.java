package xyz.juandiii.spring;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.List;

/**
 * Auto-discovers @ArkClient interfaces and registers them as beans.
 * Uses Spring Boot's auto-configuration packages as base packages.
 * Activated automatically by ArkAutoConfiguration — no @EnableArkClients needed.
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
        List<String> basePackages;
        try {
            basePackages = AutoConfigurationPackages.get(
                    (org.springframework.beans.factory.BeanFactory) registry);
        } catch (IllegalStateException e) {
            return;
        }

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

                    if (!registry.containsBeanDefinition(beanName(iface))) {
                        registerProxyBean(registry, iface, annotation);
                    }
                } catch (ClassNotFoundException ignored) {
                }
            }
        }
    }

    private void registerProxyBean(BeanDefinitionRegistry registry,
                                   Class<?> iface,
                                   xyz.juandiii.ark.proxy.RegisterArkClient annotation) {
        AbstractBeanDefinition beanDef = BeanDefinitionBuilder
                .genericBeanDefinition(ArkClientFactoryBean.class)
                .addConstructorArgValue(iface)
                .addConstructorArgReference("jsonSerializer")
                .addConstructorArgReference("environment")
                .getBeanDefinition();

        beanDef.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE,
                ResolvableType.forClass(iface));
        beanDef.setLazyInit(true);

        registry.registerBeanDefinition(beanName(iface), beanDef);
    }

    private String beanName(Class<?> iface) {
        return Character.toLowerCase(iface.getSimpleName().charAt(0))
                + iface.getSimpleName().substring(1);
    }
}