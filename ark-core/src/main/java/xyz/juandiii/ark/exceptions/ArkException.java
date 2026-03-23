package xyz.juandiii.ark.exceptions;

/**
 * General runtime exception for transport and configuration errors.
 *
 * @author Juan Diego Lopez V.
 */
public class ArkException extends RuntimeException {

    public ArkException(String message) {
        super(message);
    }

    public ArkException(String message, Throwable cause) {
        super(message, cause);
    }
}
