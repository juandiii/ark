package xyz.juandiii.spring.webflux;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import xyz.juandiii.ark.core.proxy.RegisterArkClient;

/**
 * Shared scanning and registration logic for reactive @RegisterArkClient interfaces.
 *
 * @author Juan Diego Lopez V.
 */
final class ArkWebFluxClientScanner {

    static final String SERIALIZER_BEAN = "jsonSerializer";
    static final String TRANSPORT_BEAN = "reactorHttpTransport";
    static final String ENVIRONMENT_BEAN = "environment";
    static final String TLS_RESOLVER_BEAN = "arkTlsResolver";
    static final String PROPERTIES_BEAN = "ark-" + ArkWebFluxProperties.class.getName();

    private ArkWebFluxClientScanner() {}

    static ClassPathScanningCandidateComponentProvider createScanner(Environment environment) {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false) {
                    @Override
                    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                        return beanDefinition.getMetadata().isInterface();
                    }
                };
        scanner.addIncludeFilter(new AnnotationTypeFilter(RegisterArkClient.class));
        scanner.setEnvironment(environment);
        return scanner;
    }

    static void registerProxyBean(BeanDefinitionRegistry registry, Class<?> iface) {
        AbstractBeanDefinition beanDef = BeanDefinitionBuilder
                .genericBeanDefinition(ArkWebFluxClientFactoryBean.class)
                .addConstructorArgValue(iface)
                .addConstructorArgReference(SERIALIZER_BEAN)
                .addConstructorArgReference(TRANSPORT_BEAN)
                .addConstructorArgReference(ENVIRONMENT_BEAN)
                .addConstructorArgReference(TLS_RESOLVER_BEAN)
                .addConstructorArgReference(PROPERTIES_BEAN)
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
