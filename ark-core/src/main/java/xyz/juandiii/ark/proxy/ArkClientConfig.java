package xyz.juandiii.ark.proxy;

import xyz.juandiii.ark.util.StringUtils;

import java.util.function.Function;

/**
 * Resolved configuration for an Ark client.
 * Resolves from: application.properties → @RegisterArkClient annotation → defaults.
 *
 * @author Juan Diego Lopez V.
 */
public final class ArkClientConfig {

    private final String baseUrl;
    private final HttpVersion httpVersion;
    private final int connectTimeout;
    private final int readTimeout;
    private final String tlsConfigurationName;

    private ArkClientConfig(String baseUrl, HttpVersion httpVersion,
                            int connectTimeout, int readTimeout,
                            String tlsConfigurationName) {
        this.baseUrl = baseUrl;
        this.httpVersion = httpVersion;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.tlsConfigurationName = tlsConfigurationName;
    }

    public String baseUrl() { return baseUrl; }
    public HttpVersion httpVersion() { return httpVersion; }
    public int connectTimeout() { return connectTimeout; }
    public int readTimeout() { return readTimeout; }
    public String tlsConfigurationName() { return tlsConfigurationName; }

    /**
     * Resolves configuration for a client interface.
     * Priority: application.properties (configKey) → properties (FQCN) → annotation → defaults.
     *
     * @param iface the client interface annotated with @RegisterArkClient
     * @param propertyLookup function to resolve properties (e.g., Environment::getProperty)
     */
    public static ArkClientConfig resolve(Class<?> iface, Function<String, String> propertyLookup) {
        RegisterArkClient annotation = iface.getAnnotation(RegisterArkClient.class);
        if (annotation == null) {
            return defaults();
        }

        String configKey = resolveConfigKey(annotation, iface);
        String prefix = "ark.client.\"" + configKey + "\".";

        String baseUrl = firstNonEmpty(
                propertyLookup.apply(prefix + "base-url"),
                annotation.baseUrl());

        HttpVersion httpVersion = parseHttpVersion(
                propertyLookup.apply(prefix + "http-version"),
                annotation.httpVersion());

        int connectTimeout = parseInt(
                propertyLookup.apply(prefix + "connect-timeout"),
                annotation.connectTimeout());

        int readTimeout = parseInt(
                propertyLookup.apply(prefix + "read-timeout"),
                annotation.readTimeout());

        String tlsConfigurationName = firstNonEmpty(
                propertyLookup.apply(prefix + "tls-configuration-name"),
                null);

        return new ArkClientConfig(baseUrl, httpVersion, connectTimeout, readTimeout, tlsConfigurationName);
    }

    public static ArkClientConfig defaults() {
        return new ArkClientConfig("", RegisterArkClient.DEFAULT_HTTP_VERSION,
                RegisterArkClient.DEFAULT_CONNECT_TIMEOUT, RegisterArkClient.DEFAULT_READ_TIMEOUT, null);
    }

    private static String resolveConfigKey(RegisterArkClient annotation, Class<?> iface) {
        return StringUtils.isNotEmpty(annotation.configKey())
                ? annotation.configKey()
                : iface.getName();
    }

    private static String firstNonEmpty(String... values) {
        for (String v : values) {
            if (StringUtils.isNotEmpty(v)) return v;
        }
        return "";
    }

    private static HttpVersion parseHttpVersion(String value, HttpVersion defaultValue) {
        if (StringUtils.isEmpty(value)) return defaultValue;
        try {
            return HttpVersion.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    private static int parseInt(String value, int defaultValue) {
        if (StringUtils.isEmpty(value)) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
