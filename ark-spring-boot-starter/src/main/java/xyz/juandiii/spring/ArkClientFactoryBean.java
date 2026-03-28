package xyz.juandiii.spring;

import java.net.http.HttpClient;
import java.time.Duration;

import javax.net.ssl.SSLContext;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.env.Environment;

import xyz.juandiii.ark.ArkClient;
import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.http.HttpTransport;
import xyz.juandiii.ark.interceptor.LoggingInterceptor;
import xyz.juandiii.ark.proxy.ArkProxy;
import xyz.juandiii.ark.proxy.HttpVersion;
import xyz.juandiii.ark.proxy.RegisterArkClient;
import xyz.juandiii.ark.proxy.TlsResolver;
import xyz.juandiii.ark.ssl.InsecureSslContext;
import xyz.juandiii.ark.transport.jdk.ArkJdkHttpTransport;
import xyz.juandiii.ark.util.StringUtils;

/**
 * Spring FactoryBean that creates an Ark proxy client.
 * Reads configuration from type-safe ArkProperties.
 *
 * @author Juan Diego Lopez V.
 */
public class ArkClientFactoryBean<T> implements FactoryBean<T> {

    private final Class<T> clientInterface;
    private final JsonSerializer serializer;
    private final HttpTransport defaultTransport;
    private final Environment environment;
    private final TlsResolver tlsResolver;
    private final ArkProperties arkProperties;

    public ArkClientFactoryBean(Class<T> clientInterface,
            JsonSerializer serializer,
            HttpTransport defaultTransport,
            Environment environment,
            TlsResolver tlsResolver,
            ArkProperties arkProperties) {
        this.clientInterface = clientInterface;
        this.serializer = serializer;
        this.defaultTransport = defaultTransport;
        this.environment = environment;
        this.tlsResolver = tlsResolver;
        this.arkProperties = arkProperties;
    }

    @Override
    public T getObject() {
        RegisterArkClient annotation = clientInterface.getAnnotation(RegisterArkClient.class);
        String configKey = StringUtils.isNotEmpty(annotation.configKey())
                ? annotation.configKey()
                : clientInterface.getName();

        ArkProperties.ClientProperties config = arkProperties.getClient().get(configKey);

        String baseUrl = config != null && StringUtils.isNotEmpty(config.getBaseUrl())
                ? config.getBaseUrl()
                : environment.resolvePlaceholders(annotation.baseUrl());
        HttpVersion httpVersion = config != null ? config.getHttpVersion() : annotation.httpVersion();
        int connectTimeout = config != null ? config.getConnectTimeout() : annotation.connectTimeout();
        int readTimeout = config != null ? config.getReadTimeout() : annotation.readTimeout();
        String tlsConfigName = config != null ? config.getTlsConfigurationName() : null;
        boolean trustAll = config != null && config.isTrustAll();

        HttpTransport transport = resolveTransport(httpVersion, connectTimeout, tlsConfigName, trustAll, configKey);

        ArkClient.Builder builder = ArkClient.builder()
                .serializer(serializer)
                .transport(transport)
                .baseUrl(baseUrl)
                .httpVersion(httpVersion)
                .connectTimeout(connectTimeout)
                .readTimeout(readTimeout)
                .requestInterceptor(ctx -> {
                    if (ctx.timeout() == null) {
                        ctx.timeout(Duration.ofSeconds(readTimeout));
                    }
                });

        LoggingInterceptor.apply(builder, arkProperties.getLogging().getLevel());

        return (T) ArkProxy.create(clientInterface, builder.build());
    }

    @Override
    public Class<?> getObjectType() {
        return clientInterface;
    }

    private HttpTransport resolveTransport(
            HttpVersion httpVersion,
            int connectTimeout,
            String tlsConfigName,
            boolean trustAll,
            String clientName) {
        SSLContext sslContext = trustAll
                ? InsecureSslContext.create(clientName)
                : resolveSsl(tlsConfigName);

        if (sslContext != null || httpVersion != HttpVersion.HTTP_1_1
                || connectTimeout != RegisterArkClient.DEFAULT_CONNECT_TIMEOUT) {
            HttpClient.Builder httpBuilder = HttpClient.newBuilder()
                    .version(httpVersion == HttpVersion.HTTP_2
                            ? HttpClient.Version.HTTP_2
                            : HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofSeconds(connectTimeout));
            if (sslContext != null) {
                httpBuilder.sslContext(sslContext);
            }
            return new ArkJdkHttpTransport(httpBuilder.build());
        }
        return defaultTransport;
    }

    private SSLContext resolveSsl(String tlsConfigurationName) {
        if (StringUtils.isEmpty(tlsConfigurationName))
            return null;
        return tlsResolver.resolve(tlsConfigurationName);
    }
}