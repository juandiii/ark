package xyz.juandiii.ark.core.exceptions;

/**
 * Base exception for HTTP error responses (status 400-599).
 *
 * @author Juan Diego Lopez V.
 */
public class ApiException extends RuntimeException {

    private final int statusCode;
    private final String responseBody;

    public ApiException(int statusCode, String responseBody) {
        super("HTTP " + statusCode + ": " + responseBody);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int statusCode() {
        return statusCode;
    }

    public String responseBody() {
        return responseBody;
    }

    public boolean isClientError() {
        return statusCode >= 400 && statusCode <= 499;
    }

    public boolean isServerError() {
        return statusCode >= 500 && statusCode <= 599;
    }

    /**
     * Creates the appropriate exception subclass for the given status code.
     */
    public static ApiException of(int statusCode, String responseBody) {
        return switch (statusCode) {
            case 400 -> new BadRequestException(responseBody);
            case 401 -> new UnauthorizedException(responseBody);
            case 403 -> new ForbiddenException(responseBody);
            case 404 -> new NotFoundException(responseBody);
            case 409 -> new ConflictException(responseBody);
            case 422 -> new UnprocessableEntityException(responseBody);
            case 429 -> new TooManyRequestsException(responseBody);
            case 500 -> new InternalServerErrorException(responseBody);
            case 502 -> new BadGatewayException(responseBody);
            case 503 -> new ServiceUnavailableException(responseBody);
            case 504 -> new GatewayTimeoutException(responseBody);
            default -> statusCode >= 500
                    ? new ServerException(statusCode, responseBody)
                    : new ClientException(statusCode, responseBody);
        };
    }
}