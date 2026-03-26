package xyz.juandiii.ark.proxy;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Utility for URL form encoding.
 *
 * @author Juan Diego Lopez V.
 */
public final class FormEncoder {

    private FormEncoder() {}

    public static String encode(Map<String, String> params) {
        StringJoiner joiner = new StringJoiner("&");
        params.forEach((key, value) ->
                joiner.add(urlEncode(key) + "=" + urlEncode(value)));
        return joiner.toString();
    }

    public static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}