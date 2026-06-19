package xyz.juandiii.spring.webflux;

import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables auto-discovery and registration of @RegisterArkClient interfaces
 * as reactive Spring beans (ReactorArk).
 *
 * @author Juan Diego Lopez V.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(ArkWebFluxClientBeanRegistrar.class)
public @interface EnableArkWebFluxClients {

    String[] value() default {};

    String[] basePackages() default {};
}
