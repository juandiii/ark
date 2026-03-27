package xyz.juandiii.ark.exceptions;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class ArkExceptionTest {

    @Nested
    class FromIOException {

        @Test
        void givenSocketTimeout_thenReturnsTimeoutException() {
            var ex = ArkException.fromIOException(new SocketTimeoutException("timed out"));
            assertInstanceOf(TimeoutException.class, ex);
        }

        @Test
        void givenHttpTimeout_thenReturnsTimeoutException() {
            var ex = ArkException.fromIOException(new HttpTimeoutException("timed out"));
            assertInstanceOf(TimeoutException.class, ex);
        }

        @Test
        void givenNestedTimeout_thenReturnsTimeoutException() {
            var cause = new SocketTimeoutException("inner");
            var ex = ArkException.fromIOException(new IOException("wrapper", cause));
            assertInstanceOf(TimeoutException.class, ex);
        }

        @Test
        void givenConnectException_thenReturnsConnectionException() {
            var ex = ArkException.fromIOException(new ConnectException("refused"));
            assertInstanceOf(ConnectionException.class, ex);
        }

        @Test
        void givenUnknownHost_thenReturnsConnectionException() {
            var ex = ArkException.fromIOException(new UnknownHostException("bad.host"));
            assertInstanceOf(ConnectionException.class, ex);
        }

        @Test
        void givenNestedConnectionError_thenReturnsConnectionException() {
            var cause = new ConnectException("refused");
            var ex = ArkException.fromIOException(new IOException("wrapper", cause));
            assertInstanceOf(ConnectionException.class, ex);
        }

        @Test
        void givenGenericIO_thenReturnsArkException() {
            var ex = ArkException.fromIOException(new IOException("something"));
            assertInstanceOf(ArkException.class, ex);
            assertFalse(ex instanceof TimeoutException);
            assertFalse(ex instanceof ConnectionException);
        }
    }

    @Nested
    class Constructors {

        @Test
        void givenMessage_thenSetsMessage() {
            var ex = new ArkException("test");
            assertEquals("test", ex.getMessage());
            assertNull(ex.method());
            assertNull(ex.uri());
        }

        @Test
        void givenMessageAndCause_thenSetsBoth() {
            var cause = new RuntimeException("cause");
            var ex = new ArkException("test", cause);
            assertEquals("test", ex.getMessage());
            assertSame(cause, ex.getCause());
        }

        @Test
        void givenMethodAndUri_thenIncludesInMessage() {
            var uri = java.net.URI.create("https://api.example.com/users");
            var ex = new ArkException("GET", uri, "timed out", new RuntimeException());
            assertEquals("GET", ex.method());
            assertEquals(uri, ex.uri());
            assertTrue(ex.getMessage().contains("GET"));
            assertTrue(ex.getMessage().contains("https://api.example.com/users"));
        }
    }

    @Nested
    class FromThrowableWithContext {

        @Test
        void givenTimeoutWithMethodUri_thenIncludes() {
            var uri = java.net.URI.create("https://api.example.com/slow");
            var ex = ArkException.fromIOException("GET", uri, new java.net.SocketTimeoutException("timed out"));
            assertInstanceOf(TimeoutException.class, ex);
            assertEquals("GET", ex.method());
            assertEquals(uri, ex.uri());
        }

        @Test
        void givenConnectionWithMethodUri_thenIncludes() {
            var uri = java.net.URI.create("https://bad.host/api");
            var ex = ArkException.fromIOException("POST", uri, new java.net.ConnectException("refused"));
            assertInstanceOf(ConnectionException.class, ex);
            assertEquals("POST", ex.method());
            assertEquals(uri, ex.uri());
        }

        @Test
        void givenFromThrowableWithMethodUri_thenIncludes() {
            var uri = java.net.URI.create("https://api.example.com/fail");
            var ex = ArkException.fromThrowable("PUT", uri, new RuntimeException("unknown"));
            assertEquals("PUT", ex.method());
            assertEquals(uri, ex.uri());
        }

        @Test
        void givenArkExceptionPassthrough_thenReturnsOriginal() {
            var original = new ArkException("original");
            var result = ArkException.fromThrowable("GET", java.net.URI.create("http://x"), original);
            assertSame(original, result);
        }

        @Test
        void givenIOExceptionFromThrowable_thenClassifies() {
            var uri = java.net.URI.create("https://api.example.com/test");
            var ex = ArkException.fromThrowable("GET", uri, new SocketTimeoutException("timeout"));
            assertInstanceOf(TimeoutException.class, ex);
        }

        @Test
        void givenTimeoutByClassName_thenDetected() {
            var uri = java.net.URI.create("https://api.example.com/test");
            var ex = ArkException.fromThrowable("GET", uri, new FakeReadTimeoutException("timed out"));
            assertInstanceOf(TimeoutException.class, ex);
        }

        @Test
        void givenConnectionByClassName_thenDetected() {
            var uri = java.net.URI.create("https://api.example.com/test");
            var ex = ArkException.fromThrowable("GET", uri, new FakeConnectException("refused"));
            assertInstanceOf(ConnectionException.class, ex);
        }

        @Test
        void givenNestedTimeout_thenDetected() {
            var uri = java.net.URI.create("https://api.example.com/test");
            var cause = new FakeReadTimeoutException("inner timeout");
            var ex = ArkException.fromThrowable("GET", uri, new RuntimeException("wrapper", cause));
            assertInstanceOf(TimeoutException.class, ex);
        }

        @Test
        void givenNestedConnection_thenDetected() {
            var uri = java.net.URI.create("https://api.example.com/test");
            var cause = new java.net.ConnectException("refused");
            var ex = ArkException.fromThrowable("GET", uri, new RuntimeException("wrapper", cause));
            assertInstanceOf(ConnectionException.class, ex);
        }
    }

    // Custom exceptions to simulate Netty/Vert.x exceptions detected by class name
    static class FakeReadTimeoutException extends RuntimeException {
        FakeReadTimeoutException(String msg) { super(msg); }
    }

    static class FakeConnectException extends RuntimeException {
        FakeConnectException(String msg) { super(msg); }
    }

    @Nested
    class SubclassConstructors {

        @Test
        void timeoutExceptionWithMethodUri() {
            var uri = java.net.URI.create("https://api.example.com/slow");
            var ex = new TimeoutException("GET", uri, "timed out", new RuntimeException());
            assertEquals("GET", ex.method());
            assertEquals(uri, ex.uri());
        }

        @Test
        void connectionExceptionWithMethodUri() {
            var uri = java.net.URI.create("https://bad.host/api");
            var ex = new ConnectionException("POST", uri, "refused", new RuntimeException());
            assertEquals("POST", ex.method());
            assertEquals(uri, ex.uri());
        }

        @Test
        void requestInterruptedExceptionWithMethodUri() {
            var uri = java.net.URI.create("https://api.example.com/test");
            var ex = new RequestInterruptedException("GET", uri, "interrupted", new InterruptedException());
            assertEquals("GET", ex.method());
            assertEquals(uri, ex.uri());
        }

        @Test
        void requestInterruptedExceptionSimple() {
            var ex = new RequestInterruptedException("interrupted", new InterruptedException());
            assertNull(ex.method());
            assertNull(ex.uri());
        }
    }
}