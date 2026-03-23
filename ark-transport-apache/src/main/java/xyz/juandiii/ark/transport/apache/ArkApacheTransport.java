package xyz.juandiii.ark.transport.apache;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.util.Timeout;
import xyz.juandiii.ark.exceptions.ApiException;
import xyz.juandiii.ark.exceptions.ArkException;
import xyz.juandiii.ark.http.HttpTransport;
import xyz.juandiii.ark.http.RawResponse;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
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

    @Override
    public RawResponse send(String method, URI uri, Map<String, String> headers,
                            String body, Duration timeout) {
        ClassicRequestBuilder builder = ClassicRequestBuilder.create(method).setUri(uri);

        headers.forEach(builder::addHeader);

        if (body != null) {
            String ct = headers.getOrDefault("Content-Type", "application/json");
            builder.setEntity(new StringEntity(body, ContentType.create(ct)));
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
                    throw new ApiException(statusCode, responseBody);
                }

                Map<String, List<String>> responseHeaders = new HashMap<>();
                for (Header header : response.getHeaders()) {
                    responseHeaders.computeIfAbsent(header.getName(), k -> new ArrayList<>())
                            .add(header.getValue());
                }

                return new RawResponse(statusCode, responseHeaders, responseBody);
            });
        } catch (ApiException e) {
            throw e;
        } catch (IOException e) {
            throw new ArkException("API request failed: " + e.getMessage(), e);
        }
    }
}
