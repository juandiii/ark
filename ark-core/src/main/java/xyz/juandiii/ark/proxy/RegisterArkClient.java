package xyz.juandiii.ark.proxy;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Registers an interface as an Ark declarative HTTP client.
 * The framework auto-creates and registers the proxy bean
 * (CDI in Quarkus, Spring bean in Spring Boot).
 *
 * <p>Supports property placeholders: {@code ${api.users.url}} or
 * {@code ${api.users.url:https://default.com}}
 *
 * @author Juan Diego Lopez V.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RegisterArkClient {

    /**
     * Base URL of the remote service.
     * Supports property placeholders: "${property.key}" or "${property.key:default}".
     */
    String baseUrl() default "";

    /**
     * HTTP protocol version.
     */
    HttpVersion httpVersion() default HttpVersion.HTTP_1_1;

    /**
     * Connection timeout in seconds.
     */
    int connectTimeout() default 10;

    /**
     * Read timeout in seconds.
     */
    int readTimeout() default 30;
}