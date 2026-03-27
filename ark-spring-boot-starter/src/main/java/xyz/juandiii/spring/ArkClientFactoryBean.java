package xyz.juandiii.spring;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.env.Environment;
import xyz.juandiii.ark.Ark;
import xyz.juandiii.ark.ArkClient;
import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.http.HttpTransport;
import xyz.juandiii.ark.interceptor.LoggingInterceptor;
import xyz.juandiii.ark.proxy.ArkClientConfig;
import xyz.juandiii.ark.proxy.ArkProxy;
import xyz.juandiii.ark.proxy.HttpVersion;
import xyz.juandiii.ark.proxy.TlsResolver;
import xyz.juandiii.ark.transport.jdk.ArkJdkHttpTransport;
import xyz.juandiii.ark.util.StringUtils;

import javax.net.ssl.SSLContext;
import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Spring FactoryBean that creates an Ark proxy client.
 * Reads configuration from application.properties via ArkClientConfig.
 *
 * @author Juan Diego Lopez V.
 */
public class ArkClientFactoryBean<T> implements FactoryBean<T> {

    private final Class<T> clientInterface;
    private final JsonSerializer serializer;
    private final HttpTransport defaultTransport;
    private final Environment environment;
    private final TlsResolver tlsResolver;

    public ArkClientFactoryBean(Class<T> clientInterface,
                                JsonSerializer serializer,
                                HttpTransport defaultTransport,
                                Environment environment,
                                TlsResolver tlsResolver) {
        this.clientInterface = clientInterface;
        this.serializer = serializer;
        this.defaultTransport = defaultTransport;
        this.environment = environment;
        this.tlsResolver = tlsResolver;
    }

    @Override
    public T getObject() {
        ArkClientConfig config = ArkClientConfig.resolve(clientInterface, environment::getProperty);

        String resolvedUrl = environment.resolvePlaceholders(config.baseUrl());
        HttpTransport transport = resolveTransport(config);

        ArkClient.Builder builder = ArkClient.builder()
                .serializer(serializer)
                .transport(transport)
                .baseUrl(resolvedUrl)
                .connectTimeout(config.connectTimeout())
                .readTimeout(config.readTimeout())
                .requestInterceptor(ctx -> {
                    if (ctx.timeout() == null) {
                        ctx.timeout(Duration.ofSeconds(config.readTimeout()));
                    }
                });

        LoggingInterceptor.apply(builder,
                LoggingInterceptor.parseLevel(environment.getProperty("ark.logging.level")));

        @SuppressWarnings("unchecked")
        T proxy = (T) ArkProxy.create(clientInterface, builder.build());
        return proxy;
    }

    @Override
    public Class<?> getObjectType() {
        return clientInterface;
    }

    private HttpTransport resolveTransport(ArkClientConfig config) {
        SSLContext sslContext = resolveSsl(config.tlsConfigurationName());

        if (sslContext != null || config.httpVersion() != HttpVersion.HTTP_1_1
                || config.connectTimeout() != 10) {
            HttpClient.Builder httpBuilder = HttpClient.newBuilder()
                    .version(config.httpVersion() == HttpVersion.HTTP_2
                            ? HttpClient.Version.HTTP_2
                            : HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofSeconds(config.connectTimeout()));
            if (sslContext != null) {
                httpBuilder.sslContext(sslContext);
            }
            return new ArkJdkHttpTransport(httpBuilder.build());
        }
        return defaultTransport;
    }

    private SSLContext resolveSsl(String tlsConfigurationName) {
        if (StringUtils.isEmpty(tlsConfigurationName) || tlsResolver == null) return null;
        return tlsResolver.resolve(tlsConfigurationName);
    }
}
