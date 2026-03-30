package xyz.juandiii.ark.transport.reactor;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufMono;
import reactor.netty.http.client.HttpClient;
import io.netty.handler.codec.http.HttpMethod;
import xyz.juandiii.ark.core.exceptions.ApiException;
import xyz.juandiii.ark.core.exceptions.ArkException;
import xyz.juandiii.ark.core.http.HeaderUtils;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.http.TransportLogger;
import xyz.juandiii.ark.reactor.http.ReactorHttpTransport;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * HTTP transport bridge using Reactor Netty HttpClient.
 *
 * @author Juan Diego Lopez V.
 */
public final class ArkReactorNettyTransport implements ReactorHttpTransport {

    private final HttpClient httpClient;

    public ArkReactorNettyTransport(HttpClient httpClient) {
        Objects.requireNonNull(httpClient, "HttpClient is required");
        this.httpClient = httpClient;
    }

    private static final System.Logger LOGGER = System.getLogger(ArkReactorNettyTransport.class.getName());

    @Override
    public Mono<RawResponse> send(String method, URI uri, Map<String, String> headers,
                                  String body, Duration timeout) {
        LOGGER.log(System.Logger.Level.DEBUG, () -> TransportLogger.formatRequest(method, uri, headers, body));
        return execute(method, uri, headers,
                body != null ? ByteBufMono.fromString(Mono.just(body)) : Mono.empty(),
                timeout);
    }

    @Override
    public Mono<RawResponse> sendBinary(String method, URI uri, Map<String, String> headers,
                                         byte[] body, Duration timeout) {
        LOGGER.log(System.Logger.Level.DEBUG, () ->
                TransportLogger.formatRequest(method, uri, headers, body != null ? "[binary: " + body.length + " bytes]" : null));
        return execute(method, uri, headers,
                body != null ? Mono.just(Unpooled.wrappedBuffer(body)) : Mono.empty(),
                timeout);
    }

    private Mono<RawResponse> execute(String method, URI uri, Map<String, String> headers,
                                       Publisher<? extends ByteBuf> bodyPublisher, Duration timeout) {
        Mono<RawResponse> result = httpClient
                .headers(h -> headers.forEach(h::set))
                .request(HttpMethod.valueOf(method))
                .uri(uri)
                .send(bodyPublisher)
                .responseSingle((response, content) ->
                        content.asString().defaultIfEmpty("").map(responseBody -> {
                            int statusCode = response.status().code();
                            var responseHeaders = HeaderUtils.toHeaderMap(response.responseHeaders());
                            LOGGER.log(System.Logger.Level.DEBUG, () ->
                                    TransportLogger.formatResponse(statusCode, responseHeaders, responseBody));
                            if (RawResponse.isErrorStatus(statusCode)) {
                                throw ApiException.of(statusCode, responseBody);
                            }
                            return new RawResponse(statusCode, responseHeaders, responseBody);
                        })
                );

        if (timeout != null) {
            result = result.timeout(timeout);
        }

        return result.onErrorMap(e -> !(e instanceof ArkException || e instanceof ApiException),
                e -> ArkException.fromThrowable(method, uri, e));
    }
}
