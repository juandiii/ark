package xyz.juandiii.ark.core.exceptions;

import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;

/**
 * General runtime exception for transport and configuration errors.
 *
 * @author Juan Diego Lopez V.
 */
public class ArkException extends RuntimeException {

    private final String method;
    private final URI uri;

    public ArkException(String message) {
        super(message);
        this.method = null;
        this.uri = null;
    }

    public ArkException(String message, Throwable cause) {
        super(message, cause);
        this.method = null;
        this.uri = null;
    }

    public ArkException(String method, URI uri, String message, Throwable cause) {
        super(method + " " + uri + " - " + message, cause);
        this.method = method;
        this.uri = uri;
    }

    public String method() {
        return method;
    }

    public URI uri() {
        return uri;
    }

    /**
     * Creates the appropriate exception subclass for the given IOException.
     */
    public static ArkException fromIOException(String method, URI uri, IOException e) {
        if (isTimeout(e)) {
            return new TimeoutException(method, uri, "Request timed out: " + e.getMessage(), e);
        }
        if (isConnectionError(e)) {
            return new ConnectionException(method, uri, "Connection failed: " + e.getMessage(), e);
        }
        return new ArkException(method, uri, "Request failed: " + e.getMessage(), e);
    }

    /**
     * Creates the appropriate exception subclass for any Throwable.
     */
    public static ArkException fromThrowable(String method, URI uri, Throwable e) {
        if (e instanceof ArkException ark) return ark;
        if (e instanceof IOException io) return fromIOException(method, uri, io);
        if (isTimeout(e)) return new TimeoutException(method, uri, "Request timed out: " + e.getMessage(), e);
        if (isConnectionError(e)) return new ConnectionException(method, uri, "Connection failed: " + e.getMessage(), e);
        return new ArkException(method, uri, "Request failed: " + e.getMessage(), e);
    }

    /**
     * @deprecated Use {@link #fromIOException(String, URI, IOException)} instead.
     */
    @Deprecated
    public static ArkException fromIOException(IOException e) {
        return fromIOException(null, null, e);
    }

    /**
     * @deprecated Use {@link #fromThrowable(String, URI, Throwable)} instead.
     */
    @Deprecated
    public static ArkException fromThrowable(Throwable e) {
        return fromThrowable(null, null, e);
    }

    private static boolean isTimeout(Throwable e) {
        if (e instanceof SocketTimeoutException
                || e instanceof HttpTimeoutException
                || e instanceof HttpConnectTimeoutException) {
            return true;
        }
        String name = e.getClass().getSimpleName();
        if (name.contains("Timeout") || name.contains("timeout")) {
            return true;
        }
        if (e.getCause() != null && e.getCause() != e) {
            return isTimeout(e.getCause());
        }
        return false;
    }

    private static boolean isConnectionError(Throwable e) {
        if (e instanceof ConnectException
                || e instanceof UnknownHostException
                || e instanceof NoRouteToHostException) {
            return true;
        }
        String name = e.getClass().getSimpleName();
        if (name.contains("ConnectException") || name.contains("ConnectionRefused")
                || name.contains("UnresolvedAddress")) {
            return true;
        }
        if (e.getCause() != null && e.getCause() != e) {
            return isConnectionError(e.getCause());
        }
        return false;
    }
}