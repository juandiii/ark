package xyz.juandiii.ark.spring.clients;

import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import xyz.juandiii.ark.core.proxy.RegisterArkClient;

@HttpExchange
@RegisterArkClient(configKey = "sync-api")
public interface SyncApi {

    @GetExchange("/status")
    String status();
}
