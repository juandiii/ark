package xyz.juandiii.ark.proxy.spring;

import xyz.juandiii.ark.proxy.AnnotationResolver;
import xyz.juandiii.ark.proxy.ArkProxy;
import xyz.juandiii.ark.proxy.ParameterBinder;

/**
 * Provides Spring annotation resolver and parameter binder for ArkProxy auto-detection.
 *
 * @author Juan Diego Lopez V.
 */
public final class SpringProxyProvider implements ArkProxy.ProxyProviderAccess {

    @Override
    public AnnotationResolver annotationResolver() {
        return new SpringAnnotationResolver();
    }

    @Override
    public ParameterBinder parameterBinder() {
        return new SpringParameterBinder();
    }
}