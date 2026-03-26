package xyz.juandiii.spring;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.core.type.filter.AnnotationTypeFilter;

/**
 * Shared scanning and registration logic for @RegisterArkClient interfaces.
 *
 * @author Juan Diego Lopez V.
 */
final class ArkClientScanner {

    static final String SERIALIZER_BEAN = "jsonSerializer";
    static final String TRANSPORT_BEAN = "httpTransport";
    static final String ENVIRONMENT_BEAN = "environment";

    private ArkClientScanner() {}

    static ClassPathScanningCandidateComponentProvider createScanner(Environment environment) {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false) {
                    @Override
                    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                        return beanDefinition.getMetadata().isInterface();
                    }
                };
        scanner.addIncludeFilter(new AnnotationTypeFilter(xyz.juandiii.ark.proxy.RegisterArkClient.class));
        scanner.setEnvironment(environment);
        return scanner;
    }

    static void registerProxyBean(BeanDefinitionRegistry registry, Class<?> iface) {
        AbstractBeanDefinition beanDef = BeanDefinitionBuilder
                .genericBeanDefinition(ArkClientFactoryBean.class)
                .addConstructorArgValue(iface)
                .addConstructorArgReference(SERIALIZER_BEAN)
                .addConstructorArgReference(TRANSPORT_BEAN)
                .addConstructorArgReference(ENVIRONMENT_BEAN)
                .getBeanDefinition();

        beanDef.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE,
                ResolvableType.forClass(iface));
        beanDef.setLazyInit(true);

        registry.registerBeanDefinition(beanName(iface), beanDef);
    }

    static String beanName(Class<?> iface) {
        return Character.toLowerCase(iface.getSimpleName().charAt(0))
                + iface.getSimpleName().substring(1);
    }
}
