package xyz.juandiii.ark.core.exceptions;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiExceptionTest {

    @Nested
    class Base {

        @Test
        void carriesStatusCodeAndBody() {
            ApiException ex = new ApiException(404, "Not Found");
            assertEquals(404, ex.statusCode());
            assertEquals("Not Found", ex.responseBody());
        }

        @Test
        void messageContainsStatusAndBody() {
            ApiException ex = new ApiException(500, "Server Error");
            assertTrue(ex.getMessage().contains("500"));
            assertTrue(ex.getMessage().contains("Server Error"));
        }

        @Test
        void isClientError() {
            assertTrue(new ApiException(400, "").isClientError());
            assertTrue(new ApiException(499, "").isClientError());
            assertFalse(new ApiException(500, "").isClientError());
        }

        @Test
        void isServerError() {
            assertTrue(new ApiException(500, "").isServerError());
            assertTrue(new ApiException(599, "").isServerError());
            assertFalse(new ApiException(400, "").isServerError());
        }
    }

    @Nested
    class Factory {

        @Test
        void givenBadRequest_thenReturnsBadRequestException() {
            assertInstanceOf(BadRequestException.class, ApiException.of(400, "bad"));
        }

        @Test
        void givenUnauthorized_thenReturnsUnauthorizedException() {
            assertInstanceOf(UnauthorizedException.class, ApiException.of(401, "unauth"));
        }

        @Test
        void givenForbidden_thenReturnsForbiddenException() {
            assertInstanceOf(ForbiddenException.class, ApiException.of(403, "forbidden"));
        }

        @Test
        void givenNotFound_thenReturnsNotFoundException() {
            assertInstanceOf(NotFoundException.class, ApiException.of(404, "not found"));
        }

        @Test
        void givenConflict_thenReturnsConflictException() {
            assertInstanceOf(ConflictException.class, ApiException.of(409, "conflict"));
        }

        @Test
        void givenUnprocessableEntity_thenReturnsUnprocessableEntityException() {
            assertInstanceOf(UnprocessableEntityException.class, ApiException.of(422, "invalid"));
        }

        @Test
        void givenTooManyRequests_thenReturnsTooManyRequestsException() {
            assertInstanceOf(TooManyRequestsException.class, ApiException.of(429, "slow down"));
        }

        @Test
        void givenInternalServerError_thenReturnsInternalServerErrorException() {
            assertInstanceOf(InternalServerErrorException.class, ApiException.of(500, "error"));
        }

        @Test
        void givenBadGateway_thenReturnsBadGatewayException() {
            assertInstanceOf(BadGatewayException.class, ApiException.of(502, "bad gw"));
        }

        @Test
        void givenServiceUnavailable_thenReturnsServiceUnavailableException() {
            assertInstanceOf(ServiceUnavailableException.class, ApiException.of(503, "unavailable"));
        }

        @Test
        void givenGatewayTimeout_thenReturnsGatewayTimeoutException() {
            assertInstanceOf(GatewayTimeoutException.class, ApiException.of(504, "timeout"));
        }

        @Test
        void givenUnknown4xx_thenReturnsClientException() {
            ApiException ex = ApiException.of(418, "teapot");
            assertInstanceOf(ClientException.class, ex);
            assertEquals(418, ex.statusCode());
        }

        @Test
        void givenUnknown5xx_thenReturnsServerException() {
            ApiException ex = ApiException.of(599, "unknown");
            assertInstanceOf(ServerException.class, ex);
            assertEquals(599, ex.statusCode());
        }
    }

    @Nested
    class Hierarchy {

        @Test
        void clientExceptionExtendsApiException() {
            assertInstanceOf(ApiException.class, ApiException.of(400, ""));
        }

        @Test
        void serverExceptionExtendsApiException() {
            assertInstanceOf(ApiException.class, ApiException.of(500, ""));
        }

        @Test
        void badRequestExtendsClientException() {
            assertInstanceOf(ClientException.class, ApiException.of(400, ""));
        }

        @Test
        void internalServerErrorExtendsServerException() {
            assertInstanceOf(ServerException.class, ApiException.of(500, ""));
        }
    }
}