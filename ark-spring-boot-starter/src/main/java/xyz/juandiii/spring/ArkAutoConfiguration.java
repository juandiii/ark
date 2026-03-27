package xyz.juandiii.spring;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.context.annotation.Scope;
import tools.jackson.databind.ObjectMapper;
import xyz.juandiii.ark.ArkClient;
import xyz.juandiii.ark.jackson.JacksonSerializer;
import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.http.HttpTransport;
import xyz.juandiii.ark.proxy.TlsResolver;
import xyz.juandiii.ark.transport.jdk.ArkJdkHttpTransport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.ssl.SslBundles;

import java.net.http.HttpClient;

/**
 * Spring Boot auto-configuration for sync Ark HTTP client.
 *
 * @author Juan Diego Lopez V.
 */
@AutoConfiguration
@Import(ArkClientAutoRegistrar.class)
@ImportRuntimeHints(ArkClientRuntimeHints.class)
public class ArkAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JsonSerializer jsonSerializer(ObjectMapper objectMapper) {
        return new JacksonSerializer(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(HttpTransport.class)
    public HttpTransport httpTransport() {
        return new ArkJdkHttpTransport(HttpClient.newBuilder().build());
    }

    @Bean
    @Scope("prototype")
    @ConditionalOnMissingBean
    public ArkClient.Builder arkClientBuilder(JsonSerializer serializer, HttpTransport transport) {
        return ArkClient.builder()
                .serializer(serializer)
                .transport(transport);
    }

    @Bean
    @ConditionalOnBean(SslBundles.class)
    @ConditionalOnMissingBean(TlsResolver.class)
    public TlsResolver arkTlsResolver(SslBundles sslBundles) {
        return new SpringTlsResolver(sslBundles);
    }
}