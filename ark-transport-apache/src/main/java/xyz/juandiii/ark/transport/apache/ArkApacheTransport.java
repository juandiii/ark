package xyz.juandiii.ark.transport.apache;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.util.Timeout;
import xyz.juandiii.ark.core.exceptions.ApiException;
import xyz.juandiii.ark.core.exceptions.ArkException;
import xyz.juandiii.ark.core.http.HeaderUtils;
import xyz.juandiii.ark.core.http.HttpTransport;
import xyz.juandiii.ark.core.http.RawResponse;
import xyz.juandiii.ark.core.http.TransportLogger;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * HTTP transport bridge using Apache HttpClient 5.
 *
 * @author Juan Diego Lopez V.
 */
public final class ArkApacheTransport implements HttpTransport {

    private final CloseableHttpClient httpClient;

    public ArkApacheTransport(CloseableHttpClient httpClient) {
        Objects.requireNonNull(httpClient, "CloseableHttpClient is required");
        this.httpClient = httpClient;
    }

    private static final System.Logger LOGGER = System.getLogger(ArkApacheTransport.class.getName());

    @Override
    public RawResponse send(String method, URI uri, Map<String, String> headers,
                            String body, Duration timeout) {
        LOGGER.log(System.Logger.Level.DEBUG, () -> TransportLogger.formatRequest(method, uri, headers, body));
        String ct = headers.getOrDefault("Content-Type", "application/json");
        HttpEntity entity = body != null ? new StringEntity(body, ContentType.create(ct)) : null;
        return execute(method, uri, headers, entity, timeout);
    }

    @Override
    public RawResponse sendBinary(String method, URI uri, Map<String, String> headers,
                                   byte[] body, Duration timeout) {
        LOGGER.log(System.Logger.Level.DEBUG, () ->
                TransportLogger.formatRequest(method, uri, headers, body != null ? "[binary: " + body.length + " bytes]" : null));
        String ct = headers.getOrDefault("Content-Type", "application/octet-stream");
        HttpEntity entity = body != null ? new ByteArrayEntity(body, ContentType.create(ct)) : null;
        return execute(method, uri, headers, entity, timeout);
    }

    private RawResponse execute(String method, URI uri, Map<String, String> headers,
                                 HttpEntity entity, Duration timeout) {
        ClassicRequestBuilder builder = ClassicRequestBuilder.create(method).setUri(uri);
        headers.forEach(builder::addHeader);

        if (entity != null) {
            builder.setEntity(entity);
        }

        HttpClientContext context = HttpClientContext.create();
        if (timeout != null) {
            context.setRequestConfig(RequestConfig.custom()
                    .setResponseTimeout(Timeout.ofMilliseconds(timeout.toMillis()))
                    .build());
        }

        try {
            return httpClient.execute(builder.build(), context, response -> {
                int statusCode = response.getCode();
                String responseBody = EntityUtils.toString(response.getEntity());

                if (RawResponse.isErrorStatus(statusCode)) {
                    throw ApiException.of(statusCode, responseBody);
                }

                Map<String, List<String>> responseHeaders = HeaderUtils.toHeaderMap(
                        java.util.Arrays.stream(response.getHeaders())
                                .map(h -> Map.entry(h.getName(), h.getValue()))
                                .toList());

                LOGGER.log(System.Logger.Level.DEBUG, () -> TransportLogger.formatResponse(statusCode, responseHeaders, responseBody));
                return new RawResponse(statusCode, responseHeaders, responseBody);
            });
        } catch (ApiException e) {
            throw e;
        } catch (IOException e) {
            throw ArkException.fromIOException(method, uri, e);
        }
    }
}
