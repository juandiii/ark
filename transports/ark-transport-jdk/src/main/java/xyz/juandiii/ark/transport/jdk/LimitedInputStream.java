package xyz.juandiii.ark.transport.jdk;

import xyz.juandiii.ark.core.exceptions.ResponseTooLargeException;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * Counts bytes read and, once the cumulative count exceeds the limit, closes the
 * underlying stream and throws {@link ResponseTooLargeException}.
 *
 * @author Juan Diego Lopez V.
 */
final class LimitedInputStream extends FilterInputStream {

    private final long maxBytes;
    private final String method;
    private final URI uri;
    private long count;

    LimitedInputStream(InputStream in, long maxBytes, String method, URI uri) {
        super(in);
        this.maxBytes = maxBytes;
        this.method = method;
        this.uri = uri;
    }

    @Override
    public int read() throws IOException {
        int b = super.read();
        if (b >= 0) add(1);
        return b;
    }

    @Override
    public int read(byte[] buf, int off, int len) throws IOException {
        int n = super.read(buf, off, len);
        if (n > 0) add(n);
        return n;
    }

    @Override
    public long skip(long n) throws IOException {
        long skipped = super.skip(n);
        if (skipped > 0) add(skipped);
        return skipped;
    }

    private void add(long n) {
        count += n;
        if (count > maxBytes) {
            try {
                in.close();
            } catch (IOException ignored) {
                // best effort
            }
            throw new ResponseTooLargeException(method, uri, maxBytes);
        }
    }
}
