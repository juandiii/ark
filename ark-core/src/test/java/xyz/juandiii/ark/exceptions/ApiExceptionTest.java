package xyz.juandiii.ark.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiExceptionTest {

    @Test
    void carriesStatusCodeAndBody() {
        ApiException ex = new ApiException(404, "Not Found");
        assertEquals(404, ex.statusCode());
        assertEquals("Not Found", ex.responseBody());
    }

    @Test
    void isUnauthorized() {
        assertTrue(new ApiException(401, "").isUnauthorized());
        assertFalse(new ApiException(403, "").isUnauthorized());
    }

    @Test
    void isNotFound() {
        assertTrue(new ApiException(404, "").isNotFound());
        assertFalse(new ApiException(400, "").isNotFound());
    }

    @Test
    void messageContainsStatusAndBody() {
        ApiException ex = new ApiException(500, "Server Error");
        assertTrue(ex.getMessage().contains("500"));
        assertTrue(ex.getMessage().contains("Server Error"));
    }
}