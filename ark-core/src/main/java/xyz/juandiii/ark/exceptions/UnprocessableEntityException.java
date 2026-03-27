package xyz.juandiii.ark.exceptions;

/**
 * Exception for HTTP 422 Unprocessable Entity.
 *
 * @author Juan Diego Lopez V.
 */
public class UnprocessableEntityException extends ClientException {

    public UnprocessableEntityException(String responseBody) {
        super(422, responseBody);
    }
}