package xyz.juandiii.ark.spring;

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

import java.lang.reflect.Method;

/**
 * Shared scanning and registration logic for @RegisterArkClient interfaces.
 *
 * @author Juan Diego Lopez V.
 */
final class ArkClientScanner {

    static final String ARK_CLIENTS_BEAN = "arkClients";

    private ArkClientScanner() {}

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
                .genericBeanDefinition(ArkClientFactoryBean.class)
                .addConstructorArgValue(iface)
                .addConstructorArgReference(ARK_CLIENTS_BEAN)
                .getBeanDefinition();

        beanDef.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE,
                ResolvableType.forClass(iface));
        beanDef.setLazyInit(true);

        registry.registerBeanDefinition(beanName(iface), beanDef);
    }

    /**
     * Interfaces returning Mono/Flux belong to the webflux starter; this starter skips them.
     */
    static boolean usesReactorReturnTypes(Class<?> iface) {
        for (Method method : iface.getMethods()) {
            String name = method.getReturnType().getName();
            if (name.equals("reactor.core.publisher.Mono") || name.equals("reactor.core.publisher.Flux")) {
                return true;
            }
        }
        return false;
    }

    static String beanName(Class<?> iface) {
        return Character.toLowerCase(iface.getSimpleName().charAt(0))
                + iface.getSimpleName().substring(1);
    }
}
