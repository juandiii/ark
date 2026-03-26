package xyz.juandiii.spring;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.env.Environment;
import xyz.juandiii.ark.Ark;
import xyz.juandiii.ark.ArkClient;
import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.proxy.ArkProxy;
import xyz.juandiii.ark.proxy.HttpVersion;
import xyz.juandiii.ark.proxy.PropertyResolver;
import xyz.juandiii.ark.transport.jdk.ArkJdkHttpTransport;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Spring FactoryBean that creates an Ark proxy client.
 * Compatible with Spring AOT/native compilation.
 *
 * @author Juan Diego Lopez V.
 */
public class ArkClientFactoryBean<T> implements FactoryBean<T> {

    private final Class<T> clientInterface;
    private final String baseUrl;
    private final HttpVersion httpVersion;
    private final int connectTimeout;
    private final int readTimeout;
    private final JsonSerializer serializer;
    private final Environment environment;

    public ArkClientFactoryBean(Class<T> clientInterface, String baseUrl,
                                HttpVersion httpVersion, int connectTimeout,
                                int readTimeout, JsonSerializer serializer,
                                Environment environment) {
        this.clientInterface = clientInterface;
        this.baseUrl = baseUrl;
        this.httpVersion = httpVersion;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.serializer = serializer;
        this.environment = environment;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getObject() {
        String resolvedUrl = PropertyResolver.resolve(baseUrl,
                environment::getProperty);

        HttpClient httpClient = HttpClient.newBuilder()
                .version(httpVersion == HttpVersion.HTTP_2
                        ? HttpClient.Version.HTTP_2
                        : HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(connectTimeout))
                .build();

        Ark ark = ArkClient.builder()
                .serializer(serializer)
                .transport(new ArkJdkHttpTransport(httpClient))
                .baseUrl(resolvedUrl)
                .requestInterceptor(ctx -> {
                    if (ctx.timeout() == null) {
                        ctx.timeout(Duration.ofSeconds(readTimeout));
                    }
                })
                .build();

        return (T) ArkProxy.create(clientInterface, ark);
    }

    @Override
    public Class<?> getObjectType() {
        return clientInterface;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}