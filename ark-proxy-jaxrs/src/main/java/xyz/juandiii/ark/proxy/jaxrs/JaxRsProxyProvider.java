package xyz.juandiii.ark.proxy.jaxrs;

import xyz.juandiii.ark.proxy.AnnotationResolver;
import xyz.juandiii.ark.proxy.ArkProxy;
import xyz.juandiii.ark.proxy.ParameterBinder;

/**
 * Provides JAX-RS annotation resolver and parameter binder for ArkProxy auto-detection.
 *
 * @author Juan Diego Lopez V.
 */
public final class JaxRsProxyProvider implements ArkProxy.ProxyProviderAccess {

    @Override
    public AnnotationResolver annotationResolver() {
        return new JaxRsAnnotationResolver();
    }

    @Override
    public ParameterBinder parameterBinder() {
        return new JaxRsParameterBinder();
    }
}