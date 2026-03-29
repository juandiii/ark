package xyz.juandiii.ark.core.http;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TransportLoggerTest {

    @Nested
    class FormatRequest {

        @Test
        void givenGetRequest_thenFormatsWithMethodAndUrl() {
            String result = TransportLogger.formatRequest("GET",
                    URI.create("https://api.example.com/users"), Map.of(), null);

            assertTrue(result.contains("Method: GET"));
            assertTrue(result.contains("URL: https://api.example.com/users"));
            assertTrue(result.contains("Host: api.example.com"));
            assertTrue(result.contains("Scheme: https"));
        }

        @Test
        void givenHeaders_thenIncludesHeaders() {
            String result = TransportLogger.formatRequest("POST",
                    URI.create("https://api.example.com/users"),
                    Map.of("Content-Type", "application/json"), null);

            assertTrue(result.contains("Headers:"));
            assertTrue(result.contains("Content-Type: application/json"));
        }

        @Test
        void givenBody_thenIncludesBody() {
            String result = TransportLogger.formatRequest("POST",
                    URI.create("https://api.example.com/users"),
                    Map.of(), "{\"name\":\"Juan\"}");

            assertTrue(result.contains("Body: {\"name\":\"Juan\"}"));
        }

        @Test
        void givenPortAndQuery_thenIncludes() {
            String result = TransportLogger.formatRequest("GET",
                    URI.create("https://api.example.com:8080/users?page=1"),
                    Map.of(), null);

            assertTrue(result.contains("Port: 8080"));
            assertTrue(result.contains("Query: page=1"));
            assertTrue(result.contains("Path: /users"));
        }

        @Test
        void givenNoQuery_thenShowsNone() {
            String result = TransportLogger.formatRequest("GET",
                    URI.create("https://api.example.com/users"),
                    Map.of(), null);

            assertTrue(result.contains("Query: none"));
        }
    }

    @Nested
    class FormatResponse {

        @Test
        void givenStatus_thenFormatsWithStatus() {
            String result = TransportLogger.formatResponse(200, Map.of(), "{\"ok\":true}");

            assertTrue(result.contains("Status: 200"));
        }

        @Test
        void givenHeaders_thenIncludesHeaders() {
            String result = TransportLogger.formatResponse(200,
                    Map.of("Content-Type", List.of("application/json")), null);

            assertTrue(result.contains("Headers:"));
            assertTrue(result.contains("Content-Type: application/json"));
        }

        @Test
        void givenBody_thenIncludesBody() {
            String result = TransportLogger.formatResponse(200, Map.of(), "{\"id\":1}");

            assertTrue(result.contains("Body: {\"id\":1}"));
        }

        @Test
        void givenNullHeaders_thenNoHeaders() {
            String result = TransportLogger.formatResponse(204, null, null);

            assertTrue(result.contains("Status: 204"));
            assertFalse(result.contains("Headers:"));
        }
    }
}