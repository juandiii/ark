package xyz.juandiii.spring;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import tools.jackson.databind.ObjectMapper;
import xyz.juandiii.ark.ArkClient;
import xyz.juandiii.ark.JacksonSerializer;
import xyz.juandiii.ark.JsonSerializer;
import xyz.juandiii.ark.transport.jdk.NativeHttpTransport;

import java.net.http.HttpClient;

@AutoConfiguration
public class ArkAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JsonSerializer jsonSerializer(ObjectMapper objectMapper) {
        return new JacksonSerializer(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public NativeHttpTransport nativeHttpTransport() {
        return new NativeHttpTransport(HttpClient.newBuilder().build());
    }

    @Bean
    @Scope("prototype")
    @ConditionalOnMissingBean(name = "syncArkBuilder")
    public ArkClient.SyncBuilder syncArkBuilder(JsonSerializer serializer, NativeHttpTransport transport) {
        return ArkClient.sync()
                .serializer(serializer)
                .transport(transport);
    }

    @Bean
    @Scope("prototype")
    @ConditionalOnMissingBean(name = "asyncArkBuilder")
    public ArkClient.AsyncBuilder asyncArkBuilder(JsonSerializer serializer, NativeHttpTransport transport) {
        return ArkClient.async()
                .serializer(serializer)
                .transport(transport);
    }
}
