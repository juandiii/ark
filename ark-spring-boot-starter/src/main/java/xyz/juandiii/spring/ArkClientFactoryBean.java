package xyz.juandiii.spring;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.env.Environment;
import xyz.juandiii.ark.Ark;
import xyz.juandiii.ark.ArkClient;
import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.http.HttpTransport;
import xyz.juandiii.ark.interceptor.LoggingInterceptor;
import xyz.juandiii.ark.proxy.ArkProxy;
import xyz.juandiii.ark.proxy.HttpVersion;
import xyz.juandiii.ark.proxy.RegisterArkClient;
import xyz.juandiii.ark.transport.jdk.ArkJdkHttpTransport;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Spring FactoryBean that creates an Ark proxy client.
 * Reads @RegisterArkClient annotation at runtime to avoid
 * property placeholder resolution at AOT/build time.
 *
 * @author Juan Diego Lopez V.
 */
public class ArkClientFactoryBean<T> implements FactoryBean<T> {

    private final Class<T> clientInterface;
    private final JsonSerializer serializer;
    private final HttpTransport defaultTransport;
    private final Environment environment;

    public ArkClientFactoryBean(Class<T> clientInterface,
                                JsonSerializer serializer,
                                HttpTransport defaultTransport,
                                Environment environment) {
        this.clientInterface = clientInterface;
        this.serializer = serializer;
        this.defaultTransport = defaultTransport;
        this.environment = environment;
    }

    @Override
    public T getObject() {
        RegisterArkClient annotation = clientInterface.getAnnotation(RegisterArkClient.class);

        String resolvedUrl = environment.resolvePlaceholders(annotation.baseUrl());
        HttpVersion httpVersion = annotation.httpVersion();
        int connectTimeout = annotation.connectTimeout();
        int readTimeout = annotation.readTimeout();

        HttpTransport transport = resolveTransport(httpVersion, connectTimeout);

        ArkClient.Builder builder = ArkClient.builder()
                .serializer(serializer)
                .transport(transport)
                .baseUrl(resolvedUrl)
                .requestInterceptor(ctx -> {
                    if (ctx.timeout() == null) {
                        ctx.timeout(Duration.ofSeconds(readTimeout));
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

    private HttpTransport resolveTransport(HttpVersion httpVersion, int connectTimeout) {
        boolean isDefault = httpVersion == RegisterArkClient.DEFAULT_HTTP_VERSION
                && connectTimeout == RegisterArkClient.DEFAULT_CONNECT_TIMEOUT;
        if (isDefault) {
            return defaultTransport;
        }
        return new ArkJdkHttpTransport(HttpClient.newBuilder()
                .version(httpVersion == HttpVersion.HTTP_2
                        ? HttpClient.Version.HTTP_2
                        : HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(connectTimeout))
                .build());
    }
}
