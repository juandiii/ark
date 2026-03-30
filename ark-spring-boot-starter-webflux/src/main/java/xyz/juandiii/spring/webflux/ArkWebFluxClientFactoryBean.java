package xyz.juandiii.spring.webflux;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.env.Environment;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;
import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.core.interceptor.LoggingInterceptor;
import xyz.juandiii.ark.core.proxy.ArkProxy;
import xyz.juandiii.ark.core.proxy.HttpVersion;
import xyz.juandiii.ark.core.proxy.InterceptorResolver;
import xyz.juandiii.ark.core.proxy.RegisterArkClient;
import xyz.juandiii.ark.core.proxy.TlsResolver;
import xyz.juandiii.ark.core.ssl.InsecureSslContext;
import xyz.juandiii.ark.core.util.StringUtils;
import xyz.juandiii.ark.reactor.ReactorArkClient;
import xyz.juandiii.ark.reactor.http.ReactorHttpTransport;
import xyz.juandiii.ark.transport.reactor.ArkReactorNettyTransport;

import java.time.Duration;

/**
 * Spring FactoryBean that creates a reactive Ark proxy client.
 * Reads configuration from type-safe ArkWebFluxProperties.
 *
 * @author Juan Diego Lopez V.
 */
public class ArkWebFluxClientFactoryBean<T> implements FactoryBean<T>, BeanFactoryAware {

    private final Class<T> clientInterface;
    private final JsonSerializer serializer;
    private final ReactorHttpTransport defaultTransport;
    private final Environment environment;
    private final TlsResolver tlsResolver;
    private final ArkWebFluxProperties arkProperties;
    private BeanFactory beanFactory;

    public ArkWebFluxClientFactoryBean(Class<T> clientInterface,
                                       JsonSerializer serializer,
                                       ReactorHttpTransport defaultTransport,
                                       Environment environment,
                                       TlsResolver tlsResolver,
                                       ArkWebFluxProperties arkProperties) {
        this.clientInterface = clientInterface;
        this.serializer = serializer;
        this.defaultTransport = defaultTransport;
        this.environment = environment;
        this.tlsResolver = tlsResolver;
        this.arkProperties = arkProperties;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getObject() {
        RegisterArkClient annotation = clientInterface.getAnnotation(RegisterArkClient.class);
        String configKey = StringUtils.isNotEmpty(annotation.configKey())
                ? annotation.configKey()
                : clientInterface.getName();

        ArkWebFluxProperties.ClientProperties config = arkProperties.getClient().get(configKey);

        String baseUrl = config != null && StringUtils.isNotEmpty(config.getBaseUrl())
                ? config.getBaseUrl()
                : environment.resolvePlaceholders(annotation.baseUrl());
        HttpVersion httpVersion = config != null ? config.getHttpVersion() : annotation.httpVersion();
        int connectTimeout = config != null ? config.getConnectTimeout() : annotation.connectTimeout();
        int readTimeout = config != null ? config.getReadTimeout() : annotation.readTimeout();
        String tlsConfigName = config != null ? config.getTlsConfigurationName() : null;
        boolean trustAll = config != null && config.isTrustAll();

        ReactorHttpTransport transport = resolveTransport(httpVersion, connectTimeout, readTimeout,
                tlsConfigName, trustAll, configKey);

        ReactorArkClient.Builder builder = ReactorArkClient.builder()
                .serializer(serializer)
                .transport(transport)
                .baseUrl(baseUrl)
                .httpVersion(httpVersion)
                .connectTimeout(connectTimeout)
                .readTimeout(readTimeout);

        InterceptorResolver.applyHeaders(builder, config != null ? config.getHeaders() : null);
        InterceptorResolver.applyInterceptors(builder, annotation.interceptors(), beanFactory::getBean);
        LoggingInterceptor.apply(builder, arkProperties.getLogging().getLevel());

        return (T) ArkProxy.create(clientInterface, builder.build());
    }

    @Override
    public Class<?> getObjectType() {
        return clientInterface;
    }

    private ReactorHttpTransport resolveTransport(HttpVersion httpVersion, int connectTimeout,
                                                   int readTimeout, String tlsConfigName,
                                                   boolean trustAll, String clientName) {
        boolean needsCustom = trustAll
                || StringUtils.isNotEmpty(tlsConfigName)
                || httpVersion != RegisterArkClient.DEFAULT_HTTP_VERSION
                || connectTimeout != RegisterArkClient.DEFAULT_CONNECT_TIMEOUT
                || readTimeout != RegisterArkClient.DEFAULT_READ_TIMEOUT;

        if (!needsCustom) return defaultTransport;

        HttpClient httpClient = HttpClient.create()
                .protocol(httpVersion == HttpVersion.HTTP_2 ? HttpProtocol.H2 : HttpProtocol.HTTP11)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout * 1000)
                .responseTimeout(Duration.ofSeconds(readTimeout));

        if (trustAll) {
            InsecureSslContext.warnTrustAll(clientName);
            try {
                var nettySslContext = SslContextBuilder.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .build();
                httpClient = httpClient.secure(ssl -> ssl.sslContext(nettySslContext));
            } catch (Exception e) {
                throw new xyz.juandiii.ark.core.exceptions.ArkException("Failed to configure trust-all SSL", e);
            }
        } else if (StringUtils.isNotEmpty(tlsConfigName)) {
            var jdkSslContext = tlsResolver.resolve(tlsConfigName);
            var nettySslContext = new io.netty.handler.ssl.JdkSslContext(
                    jdkSslContext, true, io.netty.handler.ssl.ClientAuth.NONE);
            httpClient = httpClient.secure(ssl -> ssl.sslContext(nettySslContext));
        }

        return new ArkReactorNettyTransport(httpClient);
    }
}
