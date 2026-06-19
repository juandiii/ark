package xyz.juandiii.ark.spring.webflux;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.boot.context.properties.bind.BindableRuntimeHintsRegistrar;

/**
 * Registers Ark reactive classes for reflection in GraalVM native image.
 *
 * Registers:
 * - Reactive proxy provider classes (Class.forName() instantiation)
 * - {@link ArkWebFluxProperties} binding hints via {@link BindableRuntimeHintsRegistrar}
 * - Enums used in binding (HttpVersion, LoggingInterceptor.Level)
 *
 * @author Juan Diego Lopez V.
 */
public class ArkWebFluxClientRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        registerIfPresent(hints, classLoader, "xyz.juandiii.ark.proxy.spring.SpringProxyProvider");
        registerIfPresent(hints, classLoader, "xyz.juandiii.ark.proxy.spring.SpringAnnotationResolver");
        registerIfPresent(hints, classLoader, "xyz.juandiii.ark.proxy.spring.SpringParameterBinder");
        registerIfPresent(hints, classLoader, "xyz.juandiii.ark.reactor.proxy.ReactorExecutionModelProvider");
        registerIfPresent(hints, classLoader, "xyz.juandiii.ark.reactor.proxy.ReactorReturnTypeHandler");

        // Register @ConfigurationProperties binding hints — handles nested classes,
        // Map<String, NestedClass>, and enum types automatically
        BindableRuntimeHintsRegistrar.forTypes(ArkWebFluxProperties.class).registerHints(hints);

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