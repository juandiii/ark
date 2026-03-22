package xyz.juandiii.spring;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import tools.jackson.databind.ObjectMapper;
import xyz.juandiii.ark.ArkClient;
import xyz.juandiii.ark.JacksonSerializer;
import xyz.juandiii.ark.JsonSerializer;


@AutoConfiguration
public class ArkAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JsonSerializer jsonSerializer(ObjectMapper objectMapper) {
        return new JacksonSerializer(objectMapper);
    }

    @Bean
    @Scope("prototype")
    @ConditionalOnMissingBean
    public ArkClient.SyncBuilder arkClientBuilder(JsonSerializer serializer) {
        return ArkClient.sync()
                .serializer(serializer);
    }
}
