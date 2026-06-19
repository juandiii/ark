package xyz.juandiii.spring;

import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables auto-discovery and registration of @ArkClient interfaces as Spring beans.
 * Scans from the annotated class's package by default.
 *
 * @author Juan Diego Lopez V.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(ArkClientBeanRegistrar.class)
public @interface EnableArkClients {

    /**
     * Base packages to scan for @ArkClient interfaces.
     * Defaults to the package of the annotated class.
     */
    String[] value() default {};

    /**
     * Alias for {@link #value()}.
     */
    String[] basePackages() default {};
}
