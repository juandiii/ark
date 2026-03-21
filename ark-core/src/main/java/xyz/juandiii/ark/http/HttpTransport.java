package xyz.juandiii.ark.http;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

@FunctionalInterface
public interface HttpTransport {

    RawResponse send(String method, URI uri, Map<String, String> headers, String body, Duration timeout);
}