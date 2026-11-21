package xyz.juandiii.ark.spring;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import xyz.juandiii.ark.core.proxy.RegisterArkClient;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Scanning and bean registration for @RegisterArkClient interfaces, shared by the Spring
 * starters. Each starter passes the interfaces it owns and the FactoryBean that builds them.
 *
 * @author Juan Diego Lopez V.
 */
public final class ArkClientRegistrations {

    private static final System.Logger LOGGER = System.getLogger(ArkClientRegistrations.class.getName());

    private ArkClientRegistrations() {}

    /**
     * Registers a lazy {@code factoryBeanClass} bean for every @RegisterArkClient interface in
     * {@code basePackages} accepted by {@code owns}, skipping interfaces that already have a bean.
     * The factory bean is constructed with the interface followed by {@code constructorBeanRefs}.
     */
    public static void register(BeanDefinitionRegistry registry, Environment environment,
                                List<String> basePackages, Predicate<Class<?>> owns,
                                Class<?> factoryBeanClass, String... constructorBeanRefs) {
        var scanner = createScanner(environment);
        for (String basePackage : basePackages) {
            for (var bd : scanner.findCandidateComponents(basePackage)) {
                try {
                    Class<?> iface = Class.forName(bd.getBeanClassName());
                    if (iface.getAnnotation(RegisterArkClient.class) == null || !owns.test(iface)) continue;
                    if (!registry.containsBeanDefinition(beanName(iface))) {
                        registerProxyBean(registry, iface, factoryBeanClass, constructorBeanRefs);
                    }
                } catch (ClassNotFoundException e) {
                    LOGGER.log(System.Logger.Level.WARNING,
                            "Failed to load @RegisterArkClient class: " + bd.getBeanClassName(), e);
                }
            }
        }
    }

    /**
     * Spring Boot's auto-configuration packages, or an empty list when none are registered.
     */
    public static List<String> autoConfigurationPackages(BeanDefinitionRegistry registry) {
        try {
            return AutoConfigurationPackages.get((BeanFactory) registry);
        } catch (IllegalStateException e) {
            return List.of();
        }
    }

    /**
     * Packages named by {@code basePackages} or {@code value} on the enable annotation, defaulting
     * to the package of the annotated class.
     */
    public static List<String> enabledPackages(AnnotationMetadata metadata,
                                               Class<? extends Annotation> enableAnnotation) {
        Map<String, Object> attrs = metadata.getAnnotationAttributes(enableAnnotation.getName());
        if (attrs != null) {
            for (String attr : new String[]{"basePackages", "value"}) {
                String[] packages = (String[]) attrs.get(attr);
                if (packages != null && packages.length > 0 && !packages[0].isEmpty()) {
                    return List.of(packages);
                }
            }
        }
        String className = metadata.getClassName();
        int lastDot = className.lastIndexOf('.');
        return List.of(lastDot > 0 ? className.substring(0, lastDot) : "");
    }

    /**
     * True if any method returns Mono/Flux. Checked by name so callers need no Reactor dependency.
     */
    public static boolean usesReactorReturnTypes(Class<?> iface) {
        for (Method method : iface.getMethods()) {
            String name = method.getReturnType().getName();
            if (name.equals("reactor.core.publisher.Mono") || name.equals("reactor.core.publisher.Flux")) {
                return true;
            }
        }
        return false;
    }

    private static ClassPathScanningCandidateComponentProvider createScanner(Environment environment) {
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

    private static void registerProxyBean(BeanDefinitionRegistry registry, Class<?> iface,
                                          Class<?> factoryBeanClass, String... constructorBeanRefs) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder
                .genericBeanDefinition(factoryBeanClass)
                .addConstructorArgValue(iface);
        for (String ref : constructorBeanRefs) {
            builder.addConstructorArgReference(ref);
        }
        AbstractBeanDefinition beanDef = builder.getBeanDefinition();
        beanDef.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, ResolvableType.forClass(iface));
        beanDef.setLazyInit(true);
        registry.registerBeanDefinition(beanName(iface), beanDef);
    }

    private static String beanName(Class<?> iface) {
        return Character.toLowerCase(iface.getSimpleName().charAt(0))
                + iface.getSimpleName().substring(1);
    }
}
