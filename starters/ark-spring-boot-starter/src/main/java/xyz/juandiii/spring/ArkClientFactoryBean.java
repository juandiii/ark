package xyz.juandiii.spring;

import java.lang.reflect.Method;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import javax.net.ssl.SSLContext;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.env.Environment;

import xyz.juandiii.ark.async.AsyncArkClient;
import xyz.juandiii.ark.async.http.decorator.AsyncRetryOps;
import xyz.juandiii.ark.core.ArkClient;
import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.core.http.HttpTransport;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.http.Transport;
import xyz.juandiii.ark.core.http.decorator.Retry;
import xyz.juandiii.ark.core.http.decorator.SyncRetryOps;
import xyz.juandiii.ark.core.interceptor.LoggingInterceptor;
import xyz.juandiii.ark.core.proxy.ArkProxy;
import xyz.juandiii.ark.core.proxy.HttpVersion;
import xyz.juandiii.ark.core.proxy.InterceptorResolver;
import xyz.juandiii.ark.core.proxy.RegisterArkClient;
import xyz.juandiii.ark.core.proxy.TlsResolver;
import xyz.juandiii.ark.core.ssl.InsecureSslContext;
import xyz.juandiii.ark.core.util.StringUtils;
import xyz.juandiii.ark.transport.jdk.ArkJdkAsyncTransport;
import xyz.juandiii.ark.transport.jdk.ArkJdkSyncTransport;

/**
 * Spring FactoryBean that creates an Ark proxy client. Detects the execution
 * model from the client interface's return types: if any method returns
 * {@link CompletableFuture}, the proxy delegates to {@code AsyncArkClient};
 * otherwise to {@code ArkClient} (sync). Reactive execution models live in
 * the webflux starter.
 *
 * @author Juan Diego Lopez V.
 */
public class ArkClientFactoryBean<T> implements FactoryBean<T>, BeanFactoryAware {

    private final Class<T> clientInterface;
    private final JsonSerializer serializer;
    private final HttpTransport defaultSyncTransport;
    private final ArkJdkAsyncTransport defaultAsyncTransport;
    private final Environment environment;
    private final TlsResolver tlsResolver;
    private final ArkProperties arkProperties;
    private BeanFactory beanFactory;

    public ArkClientFactoryBean(Class<T> clientInterface,
            JsonSerializer serializer,
            HttpTransport defaultSyncTransport,
            ArkJdkAsyncTransport defaultAsyncTransport,
            Environment environment,
            TlsResolver tlsResolver,
            ArkProperties arkProperties) {
        this.clientInterface = clientInterface;
        this.serializer = serializer;
        this.defaultSyncTransport = defaultSyncTransport;
        this.defaultAsyncTransport = defaultAsyncTransport;
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

        ArkProperties.ClientProperties config = arkProperties.client().get(configKey);

        String baseUrl = config != null && StringUtils.isNotEmpty(config.baseUrl())
                ? config.baseUrl()
                : environment.resolvePlaceholders(annotation.baseUrl());
        HttpVersion httpVersion = config != null ? config.httpVersion() : annotation.httpVersion();
        int connectTimeout = config != null ? config.connectTimeout() : annotation.connectTimeout();
        int readTimeout = config != null ? config.readTimeout() : annotation.readTimeout();
        String tlsConfigName = config != null ? config.tlsConfigurationName() : null;
        boolean trustAll = config != null && config.trustAll();

        Object arkClient = usesAsyncReturnTypes(clientInterface)
                ? buildAsync(config, baseUrl, httpVersion, connectTimeout, readTimeout,
                        tlsConfigName, trustAll, configKey, annotation)
                : buildSync(config, baseUrl, httpVersion, connectTimeout, readTimeout,
                        tlsConfigName, trustAll, configKey, annotation);

        return (T) ArkProxy.create(clientInterface, arkClient);
    }

    private ArkClient buildSync(ArkProperties.ClientProperties config, String baseUrl,
                                 HttpVersion httpVersion, int connectTimeout, int readTimeout,
                                 String tlsConfigName, boolean trustAll, String configKey,
                                 RegisterArkClient annotation) {
        HttpTransport baseTransport = resolveSyncTransport(httpVersion, connectTimeout,
                tlsConfigName, trustAll, configKey);
        Transport<RawResponse> transport = (config != null && config.retry() != null)
                ? baseTransport.with(Retry.of(config.retry().toRetryPolicy(), new SyncRetryOps()))
                : baseTransport;

        ArkClient.Builder builder = ArkClient.builder()
                .serializer(serializer)
                .transport(transport)
                .baseUrl(baseUrl)
                .httpVersion(httpVersion)
                .connectTimeout(connectTimeout)
                .readTimeout(readTimeout)
                .requestInterceptor(defaultTimeoutInterceptor(readTimeout));

        applyCommonInterceptors(builder, config, annotation);
        return (ArkClient) builder.build();
    }

    private AsyncArkClient buildAsync(ArkProperties.ClientProperties config, String baseUrl,
                                       HttpVersion httpVersion, int connectTimeout, int readTimeout,
                                       String tlsConfigName, boolean trustAll, String configKey,
                                       RegisterArkClient annotation) {
        ArkJdkAsyncTransport baseTransport = resolveAsyncTransport(httpVersion, connectTimeout,
                tlsConfigName, trustAll, configKey);
        Transport<CompletableFuture<RawResponse>> transport = (config != null && config.retry() != null)
                ? baseTransport.with(Retry.of(config.retry().toRetryPolicy(), new AsyncRetryOps()))
                : baseTransport;

        AsyncArkClient.Builder builder = AsyncArkClient.builder()
                .serializer(serializer)
                .transport(transport)
                .baseUrl(baseUrl)
                .httpVersion(httpVersion)
                .connectTimeout(connectTimeout)
                .readTimeout(readTimeout)
                .requestInterceptor(defaultTimeoutInterceptor(readTimeout));

        applyCommonInterceptors(builder, config, annotation);
        return (AsyncArkClient) builder.build();
    }

    private xyz.juandiii.ark.core.interceptor.RequestInterceptor defaultTimeoutInterceptor(int readTimeout) {
        return ctx -> {
            if (ctx.timeout() == null) {
                ctx.timeout(Duration.ofSeconds(readTimeout));
            }
        };
    }

    private <B extends xyz.juandiii.ark.core.AbstractArkBuilder<B>> void applyCommonInterceptors(
            B builder, ArkProperties.ClientProperties config, RegisterArkClient annotation) {
        InterceptorResolver.applyHeaders(builder, config != null ? config.headers() : null);
        InterceptorResolver.applyInterceptors(builder, annotation.interceptors(), beanFactory::getBean);
        LoggingInterceptor.apply(builder, arkProperties.logging().level());
    }

    @Override
    public Class<?> getObjectType() {
        return clientInterface;
    }

    private HttpTransport resolveSyncTransport(HttpVersion httpVersion, int connectTimeout,
                                                String tlsConfigName, boolean trustAll,
                                                String clientName) {
        HttpClient custom = customHttpClient(httpVersion, connectTimeout, tlsConfigName, trustAll, clientName);
        return custom != null ? new ArkJdkSyncTransport(custom) : defaultSyncTransport;
    }

    private ArkJdkAsyncTransport resolveAsyncTransport(HttpVersion httpVersion, int connectTimeout,
                                                        String tlsConfigName, boolean trustAll,
                                                        String clientName) {
        HttpClient custom = customHttpClient(httpVersion, connectTimeout, tlsConfigName, trustAll, clientName);
        return custom != null ? new ArkJdkAsyncTransport(custom) : defaultAsyncTransport;
    }

    /**
     * @return a fresh HttpClient with the per-client customizations, or {@code null}
     *         if the client should use the default transport bean.
     */
    private HttpClient customHttpClient(HttpVersion httpVersion, int connectTimeout,
                                         String tlsConfigName, boolean trustAll, String clientName) {
        SSLContext sslContext = trustAll
                ? InsecureSslContext.create(clientName)
                : resolveSsl(tlsConfigName);

        boolean needsCustom = sslContext != null
                || httpVersion != RegisterArkClient.DEFAULT_HTTP_VERSION
                || connectTimeout != RegisterArkClient.DEFAULT_CONNECT_TIMEOUT;
        if (!needsCustom) return null;

        HttpClient.Builder httpBuilder = HttpClient.newBuilder()
                .version(httpVersion == HttpVersion.HTTP_2
                        ? HttpClient.Version.HTTP_2
                        : HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(connectTimeout));
        if (sslContext != null) {
            httpBuilder.sslContext(sslContext);
        }
        return httpBuilder.build();
    }

    private SSLContext resolveSsl(String tlsConfigurationName) {
        if (StringUtils.isEmpty(tlsConfigurationName))
            return null;
        return tlsResolver.resolve(tlsConfigurationName);
    }

    static boolean usesAsyncReturnTypes(Class<?> iface) {
        for (Method method : iface.getMethods()) {
            if (method.getReturnType() == CompletableFuture.class) {
                return true;
            }
        }
        return false;
    }
}
