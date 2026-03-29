package xyz.juandiii.ark.core.http;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

/**
 * Functional interface for synchronous HTTP transport.
 *
 * @author Juan Diego Lopez V.
 */
@FunctionalInterface
public interface HttpTransport {

    RawResponse send(String method, URI uri, Map<String, String> headers, String body, Duration timeout);
}