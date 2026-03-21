package xyz.juandiii.ark.http;

import java.util.List;
import java.util.Map;

public record RawResponse(int statusCode, Map<String, List<String>> headers, String body) {
}
