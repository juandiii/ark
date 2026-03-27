package xyz.juandiii.spring;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import xyz.juandiii.ark.exceptions.ArkException;
import xyz.juandiii.ark.proxy.TlsResolver;

import javax.net.ssl.SSLContext;

/**
 * Spring implementation of TlsResolver using Spring Boot SSLBundles.
 * Resolves named SSL bundles defined in application.properties:
 * spring.ssl.bundle.pem."name".truststore.certificate=classpath:certs/ca.pem
 *
 * @author Juan Diego Lopez V.
 */
public class SpringTlsResolver implements TlsResolver {

    private final SslBundles sslBundles;

    public SpringTlsResolver(SslBundles sslBundles) {
        this.sslBundles = sslBundles;
    }

    @Override
    public SSLContext resolve(String tlsConfigurationName) {
        try {
            SslBundle bundle = sslBundles.getBundle(tlsConfigurationName);
            return bundle.createSslContext();
        } catch (Exception e) {
            throw new ArkException("Failed to resolve SSL bundle: " + tlsConfigurationName
                    + ". Define it in application.properties: spring.ssl.bundle.pem.\""
                    + tlsConfigurationName + "\".truststore.certificate=...", e);
        }
    }
}
