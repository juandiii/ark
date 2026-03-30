package xyz.juandiii.spring.webflux;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.context.annotation.Scope;
import reactor.netty.http.client.HttpClient;
import tools.jackson.databind.ObjectMapper;
import xyz.juandiii.ark.core.JsonSerializer;
import xyz.juandiii.ark.core.exceptions.ArkException;
import xyz.juandiii.ark.core.proxy.TlsResolver;
import xyz.juandiii.ark.jackson.JacksonSerializer;
import xyz.juandiii.ark.reactor.ReactorArkClient;
import xyz.juandiii.ark.reactor.http.ReactorHttpTransport;
import xyz.juandiii.ark.transport.reactor.ArkReactorNettyTransport;

/**
 * Spring Boot auto-configuration for reactive Ark HTTP client.
 *
 * @author Juan Diego Lopez V.
 */
@AutoConfiguration
@Import(ArkWebFluxClientAutoRegistrar.class)
@ImportRuntimeHints(ArkWebFluxClientRuntimeHints.class)
@EnableConfigurationProperties(ArkWebFluxProperties.class)
public class ArkWebFluxAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JsonSerializer jsonSerializer(ObjectMapper objectMapper) {
        return new JacksonSerializer(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(ReactorHttpTransport.class)
    public ReactorHttpTransport reactorHttpTransport() {
        return new ArkReactorNettyTransport(HttpClient.create());
    }

    @Bean
    @Scope("prototype")
    @ConditionalOnMissingBean
    public ReactorArkClient.Builder reactorArkBuilder(JsonSerializer serializer, ReactorHttpTransport transport) {
        return ReactorArkClient.builder()
                .serializer(serializer)
                .transport(transport);
    }

    @Bean
    @ConditionalOnMissingBean(TlsResolver.class)
    public TlsResolver arkTlsResolver(ObjectProvider<SslBundles> sslBundlesProvider) {
        SslBundles sslBundles = sslBundlesProvider.getIfAvailable();
        if (sslBundles != null) {
            return new SpringReactiveTlsResolver(sslBundles);
        }
        return name -> {
            throw new ArkException("TLS configuration '" + name
                    + "' requested but no SSL bundles are available. "
                    + "Configure spring.ssl.bundle.pem.\"" + name
                    + "\".truststore.certificate in application.properties");
        };
    }
}
