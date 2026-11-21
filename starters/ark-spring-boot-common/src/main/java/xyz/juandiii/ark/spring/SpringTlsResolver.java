package xyz.juandiii.ark.spring;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import xyz.juandiii.ark.core.exceptions.ArkException;
import xyz.juandiii.ark.core.proxy.TlsResolver;

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

    /**
     * Resolver used by the starters: SSL bundles when Spring Boot provides them, otherwise
     * a resolver that fails with a hint to configure {@code spring.ssl.bundle.*}.
     */
    public static TlsResolver of(ObjectProvider<SslBundles> sslBundlesProvider) {
        SslBundles sslBundles = sslBundlesProvider.getIfAvailable();
        if (sslBundles != null) {
            return new SpringTlsResolver(sslBundles);
        }
        return name -> {
            throw new ArkException("TLS configuration '" + name
                    + "' requested but no SSL bundles are available. "
                    + "Configure spring.ssl.bundle.pem.\"" + name
                    + "\".truststore.certificate in application.properties");
        };
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
