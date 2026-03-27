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
     * Configuration key for resolving properties from application.properties.
     * If empty, the fully qualified class name is used.
     * Properties are resolved as: ark.client."configKey".base-url, etc.
     */
    String configKey() default "";

    /**
     * Base URL of the remote service.
     * Supports property placeholders: "${property.key}" or "${property.key:default}".
     * Can be overridden by ark.client."configKey".base-url in application.properties.
     */
    String baseUrl() default "";

    /**
     * HTTP protocol version.
     */
    HttpVersion httpVersion() default HttpVersion.HTTP_1_1;

    /**
     * Connection timeout in seconds.
     */
    int connectTimeout() default DEFAULT_CONNECT_TIMEOUT;

    /**
     * Read timeout in seconds.
     */
    int readTimeout() default DEFAULT_READ_TIMEOUT;

    HttpVersion DEFAULT_HTTP_VERSION = HttpVersion.HTTP_1_1;
    int DEFAULT_CONNECT_TIMEOUT = 10;
    int DEFAULT_READ_TIMEOUT = 30;
}