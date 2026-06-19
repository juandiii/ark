package xyz.juandiii.spring;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.boot.context.properties.bind.BindableRuntimeHintsRegistrar;

/**
 * Registers Ark classes for reflection in GraalVM native image.
 *
 * Registers:
 * - Proxy provider classes (Class.forName() instantiation)
 * - {@link ArkProperties} and nested classes (for @ConfigurationProperties binding in native)
 * - Enums used in binding ({@link xyz.juandiii.ark.core.proxy.HttpVersion},
 *   {@link xyz.juandiii.ark.core.interceptor.LoggingInterceptor.Level})
 *
 * @author Juan Diego Lopez V.
 */
public class ArkClientRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        // Proxy provider classes (discovered via Class.forName)
        registerIfPresent(hints, classLoader, "xyz.juandiii.ark.proxy.spring.SpringProxyProvider");
        registerIfPresent(hints, classLoader, "xyz.juandiii.ark.proxy.spring.SpringAnnotationResolver");
        registerIfPresent(hints, classLoader, "xyz.juandiii.ark.proxy.spring.SpringParameterBinder");
        registerIfPresent(hints, classLoader, "xyz.juandiii.ark.core.proxy.SyncReturnTypeHandler");

        // Register @ConfigurationProperties binding hints — handles nested classes,
        // Map<String, NestedClass>, and enum types automatically
        BindableRuntimeHintsRegistrar.forTypes(ArkProperties.class).registerHints(hints);

        // Enums used in binding (from external packages)
        registerForBinding(hints, xyz.juandiii.ark.core.proxy.HttpVersion.class);
        registerForBinding(hints, xyz.juandiii.ark.core.interceptor.LoggingInterceptor.Level.class);
    }

    private void registerIfPresent(RuntimeHints hints, ClassLoader classLoader, String className) {
        try {
            Class.forName(className, false, classLoader);
            hints.reflection().registerType(
                    TypeReference.of(className),
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_DECLARED_METHODS);
        } catch (ClassNotFoundException ignored) {
        }
    }

    private void registerForBinding(RuntimeHints hints, Class<?> type) {
        hints.reflection().registerType(type,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_DECLARED_METHODS);
    }
}