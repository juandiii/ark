package xyz.juandiii.spring.webflux;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import reactor.netty.http.client.HttpClient;
import tools.jackson.databind.ObjectMapper;
import xyz.juandiii.ark.JacksonSerializer;
import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.reactor.ReactorArkClient;
import xyz.juandiii.ark.reactor.http.ReactorHttpTransport;
import xyz.juandiii.ark.transport.reactor.ArkReactorNettyTransport;

/**
 * Spring Boot auto-configuration for reactive Ark HTTP client.
 *
 * @author Juan Diego Lopez V.
 */
@AutoConfiguration
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
}
