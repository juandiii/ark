package xyz.juandiii.ark.spring;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.env.Environment;

import xyz.juandiii.ark.async.AsyncArkClient;
import xyz.juandiii.ark.async.http.decorator.AsyncRetryOps;
import xyz.juandiii.ark.core.AbstractArkBuilder;
import xyz.juandiii.ark.core.ArkClient;
import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.core.exceptions.ArkException;
import xyz.juandiii.ark.core.http.HttpTransport;
import xyz.juandiii.ark.core.http.RetryPolicy;
import xyz.juandiii.ark.core.http.StreamingTransport;
import xyz.juandiii.ark.core.http.decorator.Retry;
import xyz.juandiii.ark.core.http.decorator.SyncRetryOps;
import xyz.juandiii.ark.core.interceptor.LoggingInterceptor;
import xyz.juandiii.ark.core.interceptor.RequestInterceptor;
import xyz.juandiii.ark.core.proxy.HttpVersion;
import xyz.juandiii.ark.core.proxy.InterceptorResolver;
import xyz.juandiii.ark.core.proxy.RegisterArkClient;
import xyz.juandiii.ark.core.proxy.TlsResolver;
import xyz.juandiii.ark.core.ssl.InsecureSslContext;
import xyz.juandiii.ark.core.util.StringUtils;
import xyz.juandiii.ark.transport.jdk.ArkJdkAsyncTransport;
import xyz.juandiii.ark.transport.jdk.ArkJdkSyncTransport;

/**
 * Factory for fluent client builders pre-configured from {@code ark.client.<configKey>.*}
 * properties (base-url, timeouts, TLS, headers, retry, follow-redirects, throw-on-error,
 * max-response-bytes). {@code @RegisterArkClient} proxies are built from the same configuration.
 * <p>
 * A client whose settings differ from the defaults (TLS, HTTP version, connect timeout,
 * redirects, max-response-bytes) gets its own underlying HTTP client, so build a client once
 * and reuse it instead of calling the builder per request.
 *
 * @author Juan Diego Lopez V.
 */
public class ArkClients {

    private final JsonSerializer serializer;
    private final HttpTransport defaultSyncTransport;
    private final ArkJdkAsyncTransport defaultAsyncTransport;
    private final Environment environment;
    private final TlsResolver tlsResolver;
    private final ArkProperties arkProperties;
    private final BeanFactory beanFactory;

    public ArkClients(JsonSerializer serializer,
                      HttpTransport defaultSyncTransport,
                      ArkJdkAsyncTransport defaultAsyncTransport,
                      Environment environment,
                      TlsResolver tlsResolver,
                      ArkProperties arkProperties,
                      BeanFactory beanFactory) {
        this.serializer = serializer;
        this.defaultSyncTransport = defaultSyncTransport;
        this.defaultAsyncTransport = defaultAsyncTransport;
        this.environment = environment;
        this.tlsResolver = tlsResolver;
        this.arkProperties = arkProperties;
        this.beanFactory = beanFactory;
    }

    /**
     * Fluent builder pre-configured from {@code ark.client.<configKey>.*}.
     * Each call returns a new builder.
     *
     * @param configKey the key under {@code ark.client.<configKey>.*}
     * @return a configured sync builder
     * @throws ArkException if no {@code ark.client.<configKey>.*} configuration exists
     */
    public ArkClient.Builder builder(String configKey) {
        return syncBuilder(resolve(configKey, null));
    }

    /**
     * Async variant of {@link #builder(String)}.
     *
     * @param configKey the key under {@code ark.client.<configKey>.*}
     * @return a configured async builder
     * @throws ArkException if no {@code ark.client.<configKey>.*} configuration exists
     */
    public AsyncArkClient.Builder asyncBuilder(String configKey) {
        return asyncBuilder(resolve(configKey, null));
    }

    record ResolvedConfig(String clientName, String baseUrl, HttpVersion httpVersion,
                          int connectTimeout, int readTimeout, String tlsConfigName,
                          boolean trustAll, boolean throwOnError, boolean followRedirects,
                          long maxResponseBytes, Map<String, String> headers,
                          Class<?>[] interceptorClasses, RetryPolicy retryPolicy) {}

    /**
     * Properties win over annotation values. {@code annotation} is null for fluent builders,
     * which then require properties for {@code configKey}.
     */
    ResolvedConfig resolve(String configKey, RegisterArkClient annotation) {
        ArkProperties.ClientProperties config = arkProperties.client().get(configKey);
        Class<?>[] interceptors = annotation != null ? annotation.interceptors() : new Class<?>[0];
        String annotationBaseUrl = annotation != null ? environment.resolvePlaceholders(annotation.baseUrl()) : "";
        if (config == null) {
            if (annotation == null) {
                throw new ArkException("No Ark client configuration for key '" + configKey
                        + "' (expected ark.client." + configKey + ".* properties)");
            }
            return new ResolvedConfig(configKey, annotationBaseUrl, annotation.httpVersion(),
                    annotation.connectTimeout(), annotation.readTimeout(), null, false, true, true,
                    0, Map.of(), interceptors, null);
        }
        return new ResolvedConfig(
                configKey,
                StringUtils.isNotEmpty(config.baseUrl()) ? config.baseUrl() : annotationBaseUrl,
                config.httpVersion(),
                config.connectTimeout(),
                config.readTimeout(),
                config.tlsConfigurationName(),
                config.trustAll(),
                config.throwOnError(),
                config.followRedirects(),
                config.maxResponseBytes(),
                config.headers(),
                interceptors,
                config.retry() != null && config.retry().enabled() ? config.retry().toRetryPolicy() : null);
    }

    ArkClient.Builder syncBuilder(ResolvedConfig rc) {
        HttpClient custom = customHttpClient(rc);
        HttpTransport base = custom != null
                ? new ArkJdkSyncTransport(custom, rc.maxResponseBytes())
                : defaultSyncTransport;
        ArkClient.Builder builder = ArkClient.builder()
                .transport(rc.retryPolicy() != null
                        ? base.with(Retry.of(rc.retryPolicy(), new SyncRetryOps()))
                        : base);
        // The retry decorator is not a StreamingTransport; keep retrieveStream() on the unwrapped transport.
        if (base instanceof StreamingTransport streaming) {
            builder.streamingTransport(streaming);
        }
        applyCommon(builder, rc);
        return builder;
    }

    AsyncArkClient.Builder asyncBuilder(ResolvedConfig rc) {
        HttpClient custom = customHttpClient(rc);
        ArkJdkAsyncTransport base = custom != null
                ? new ArkJdkAsyncTransport(custom, custom.executor().orElse(Runnable::run), rc.maxResponseBytes())
                : defaultAsyncTransport;
        AsyncArkClient.Builder builder = AsyncArkClient.builder()
                .transport(rc.retryPolicy() != null
                        ? base.with(Retry.of(rc.retryPolicy(), new AsyncRetryOps()))
                        : base);
        applyCommon(builder, rc);
        return builder;
    }

    private <B extends AbstractArkBuilder<B>> void applyCommon(B builder, ResolvedConfig rc) {
        builder.serializer(serializer)
                .baseUrl(rc.baseUrl())
                .httpVersion(rc.httpVersion())
                .connectTimeout(rc.connectTimeout())
                .readTimeout(rc.readTimeout())
                .requestInterceptor(defaultTimeout(rc.readTimeout()))
                .throwOnError(rc.throwOnError());
        InterceptorResolver.applyHeaders(builder, rc.headers());
        InterceptorResolver.applyInterceptors(builder, rc.interceptorClasses(), beanFactory::getBean);
        LoggingInterceptor.apply(builder, arkProperties.logging().level());
    }

    private static RequestInterceptor defaultTimeout(int readTimeout) {
        return ctx -> {
            if (ctx.timeout() == null) {
                ctx.timeout(Duration.ofSeconds(readTimeout));
            }
        };
    }

    /**
     * @return a fresh HttpClient with the per-client customizations, or {@code null}
     *         if the client should use the default transport bean.
     */
    private HttpClient customHttpClient(ResolvedConfig rc) {
        SSLContext sslContext = rc.trustAll()
                ? InsecureSslContext.create(rc.clientName())
                : StringUtils.isEmpty(rc.tlsConfigName()) ? null : tlsResolver.resolve(rc.tlsConfigName());

        boolean needsCustom = sslContext != null
                || rc.httpVersion() != RegisterArkClient.DEFAULT_HTTP_VERSION
                || rc.connectTimeout() != RegisterArkClient.DEFAULT_CONNECT_TIMEOUT
                || !rc.followRedirects()
                // the size limit lives in the transport, so the shared default one cannot be reused
                || rc.maxResponseBytes() > 0;
        if (!needsCustom) return null;

        HttpClient.Builder httpBuilder = HttpClient.newBuilder()
                .version(rc.httpVersion() == HttpVersion.HTTP_2
                        ? HttpClient.Version.HTTP_2
                        : HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(rc.connectTimeout()))
                .followRedirects(rc.followRedirects() ? HttpClient.Redirect.NORMAL : HttpClient.Redirect.NEVER);
        if (sslContext != null) {
            httpBuilder.sslContext(sslContext);
        }
        return httpBuilder.build();
    }
}
