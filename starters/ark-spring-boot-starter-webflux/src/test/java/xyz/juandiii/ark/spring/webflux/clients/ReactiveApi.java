package xyz.juandiii.ark.spring.webflux.clients;

import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import reactor.core.publisher.Mono;
import xyz.juandiii.ark.core.proxy.RegisterArkClient;

@HttpExchange
@RegisterArkClient(configKey = "reactive-api")
public interface ReactiveApi {

    @GetExchange("/status")
    Mono<String> status();
}
