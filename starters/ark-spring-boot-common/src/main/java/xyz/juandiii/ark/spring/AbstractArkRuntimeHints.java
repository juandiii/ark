package xyz.juandiii.ark.spring;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.boot.context.properties.bind.BindableRuntimeHintsRegistrar;
import xyz.juandiii.ark.core.interceptor.LoggingInterceptor;
import xyz.juandiii.ark.core.proxy.HttpVersion;

/**
 * GraalVM native image hints shared by the Spring starters:
 * - Spring proxy provider classes plus the starter's execution-model classes
 *   (instantiated via Class.forName())
 * - the starter's @ConfigurationProperties type and its nested types
 * - enums used in binding ({@link HttpVersion}, {@link LoggingInterceptor.Level})
 *
 * @author Juan Diego Lopez V.
 */
public abstract class AbstractArkRuntimeHints implements RuntimeHintsRegistrar {

    private static final String[] SPRING_PROXY_CLASSES = {
            "xyz.juandiii.ark.proxy.spring.SpringProxyProvider",
            "xyz.juandiii.ark.proxy.spring.SpringAnnotationResolver",
            "xyz.juandiii.ark.proxy.spring.SpringParameterBinder"
    };

    private final Class<?> propertiesType;
    private final String[] executionModelClasses;

    protected AbstractArkRuntimeHints(Class<?> propertiesType, String... executionModelClasses) {
        this.propertiesType = propertiesType;
        this.executionModelClasses = executionModelClasses;
    }

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        for (String className : SPRING_PROXY_CLASSES) {
            registerIfPresent(hints, classLoader, className);
        }
        for (String className : executionModelClasses) {
            registerIfPresent(hints, classLoader, className);
        }
        BindableRuntimeHintsRegistrar.forTypes(propertiesType).registerHints(hints);
        registerForBinding(hints, HttpVersion.class);
        registerForBinding(hints, LoggingInterceptor.Level.class);
    }

    private static void registerIfPresent(RuntimeHints hints, ClassLoader classLoader, String className) {
        try {
            Class.forName(className, false, classLoader);
            hints.reflection().registerType(
                    TypeReference.of(className),
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_DECLARED_METHODS);
        } catch (ClassNotFoundException ignored) {
        }
    }

    private static void registerForBinding(RuntimeHints hints, Class<?> type) {
        hints.reflection().registerType(type,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_DECLARED_METHODS);
    }
}
