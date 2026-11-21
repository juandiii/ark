package xyz.juandiii.ark.spring.webflux;

import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.env.Environment;
import reactor.netty.http.HttpProtocol;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientRequest;
import reactor.netty.http.client.HttpClientResponse;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.http.Transport;
import xyz.juandiii.ark.core.http.decorator.Retry;
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
import xyz.juandiii.ark.reactor.http.decorator.ReactorRetryOps;
import xyz.juandiii.ark.transport.reactor.ArkReactorNettyTransport;

import java.time.Duration;
import java.util.function.BiPredicate;

/**
 * Spring FactoryBean that creates a reactive Ark proxy client.
 * Reads configuration from type-safe ArkWebFluxProperties.
 *
 * @author Juan Diego Lopez V.
 */
public class ArkWebFluxClientFactoryBean<T> implements FactoryBean<T>, BeanFactoryAware {

    /**
     * Follows 301/302/303/307/308 like {@code followRedirect(true)}, but never an https to http
     * downgrade, matching the JDK client's {@code Redirect.NORMAL} used by the sync starter.
     */
    static final BiPredicate<HttpClientRequest, HttpClientResponse> FOLLOW_REDIRECTS = (request, response) -> {
        int code = response.status().code();
        if (code != 301 && code != 302 && code != 303 && code != 307 && code != 308) return false;
        String location = response.responseHeaders().get(HttpHeaderNames.LOCATION);
        return location != null
                && !(request.resourceUrl().regionMatches(true, 0, "https:", 0, 6)
                        && location.regionMatches(true, 0, "http:", 0, 5));
    };

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

        ArkWebFluxProperties.ClientProperties config = arkProperties.client().get(configKey);

        String baseUrl = config != null && StringUtils.isNotEmpty(config.baseUrl())
                ? config.baseUrl()
                : environment.resolvePlaceholders(annotation.baseUrl());
        HttpVersion httpVersion = config != null ? config.httpVersion() : annotation.httpVersion();
        int connectTimeout = config != null ? config.connectTimeout() : annotation.connectTimeout();
        int readTimeout = config != null ? config.readTimeout() : annotation.readTimeout();
        String tlsConfigName = config != null ? config.tlsConfigurationName() : null;
        boolean trustAll = config != null && config.trustAll();
        boolean followRedirects = config == null || config.followRedirects();

        ReactorHttpTransport baseTransport = resolveTransport(httpVersion, connectTimeout, readTimeout,
                tlsConfigName, trustAll, followRedirects, configKey);
        Transport<Mono<RawResponse>> transport = (config != null && config.retry() != null && config.retry().enabled())
                ? baseTransport.with(Retry.of(config.retry().toRetryPolicy(), new ReactorRetryOps()))
                : baseTransport;

        ReactorArkClient.Builder builder = ReactorArkClient.builder()
                .serializer(serializer)
                .transport(transport)
                .baseUrl(baseUrl)
                .httpVersion(httpVersion)
                .connectTimeout(connectTimeout)
                .readTimeout(readTimeout)
                // The shared default transport has no response timeout, so apply read-timeout per request
                .requestInterceptor(ctx -> {
                    if (ctx.timeout() == null) {
                        ctx.timeout(Duration.ofSeconds(readTimeout));
                    }
                });

        InterceptorResolver.applyHeaders(builder, config != null ? config.headers() : null);
        InterceptorResolver.applyInterceptors(builder, annotation.interceptors(), beanFactory::getBean);
        LoggingInterceptor.apply(builder, arkProperties.logging().level());
        if (config != null) {
            builder.throwOnError(config.throwOnError());
        }

        return (T) ArkProxy.create(clientInterface, builder.build());
    }

    @Override
    public Class<?> getObjectType() {
        return clientInterface;
    }

    private ReactorHttpTransport resolveTransport(HttpVersion httpVersion, int connectTimeout,
                                                   int readTimeout, String tlsConfigName,
                                                   boolean trustAll, boolean followRedirects,
                                                   String clientName) {
        boolean needsCustom = trustAll
                || StringUtils.isNotEmpty(tlsConfigName)
                || httpVersion != RegisterArkClient.DEFAULT_HTTP_VERSION
                || connectTimeout != RegisterArkClient.DEFAULT_CONNECT_TIMEOUT
                || readTimeout != RegisterArkClient.DEFAULT_READ_TIMEOUT
                || !followRedirects;

        if (!needsCustom) return defaultTransport;

        HttpClient httpClient = HttpClient.create()
                .protocol(httpVersion == HttpVersion.HTTP_2 ? HttpProtocol.H2 : HttpProtocol.HTTP11)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout * 1000)
                .responseTimeout(Duration.ofSeconds(readTimeout));
        if (followRedirects) {
            httpClient = httpClient.followRedirect(FOLLOW_REDIRECTS);
        }

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
