package xyz.juandiii.ark.exceptions;

public class ArkException extends RuntimeException {

    public ArkException(String message) {
        super(message);
    }

    public ArkException(String message, Throwable cause) {
        super(message, cause);
    }
}
