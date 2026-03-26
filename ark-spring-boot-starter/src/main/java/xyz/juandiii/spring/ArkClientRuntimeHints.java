package xyz.juandiii.spring;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;

/**
 * Registers Ark proxy provider classes for reflection in GraalVM native image.
 *
 * @author Juan Diego Lopez V.
 */
public class ArkClientRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        registerIfPresent(hints, classLoader, "xyz.juandiii.ark.proxy.spring.SpringProxyProvider");
        registerIfPresent(hints, classLoader, "xyz.juandiii.ark.proxy.spring.SpringAnnotationResolver");
        registerIfPresent(hints, classLoader, "xyz.juandiii.ark.proxy.spring.SpringParameterBinder");
        registerIfPresent(hints, classLoader, "xyz.juandiii.ark.proxy.SyncReturnTypeHandler");
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
}