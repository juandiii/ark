package xyz.juandiii.ark.ssl;

import xyz.juandiii.ark.exceptions.ArkException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

/**
 * Creates an SSLContext that trusts all certificates.
 * <strong>WARNING: For development/testing only. Never use in production.</strong>
 *
 * @author Juan Diego Lopez V.
 */
public final class InsecureSslContext {

    private static final System.Logger LOGGER = System.getLogger(InsecureSslContext.class.getName());

    private static final String WARNING_MESSAGE =
            "⚠️  trust-all is enabled for client ''{0}''. "
                    + "All SSL certificates will be accepted without verification. "
                    + "DO NOT use this in production.";

    private InsecureSslContext() {}

    public static void warnTrustAll(String clientName) {
        LOGGER.log(System.Logger.Level.WARNING, WARNING_MESSAGE, clientName);
    }

    public static SSLContext create(String clientName) {
        warnTrustAll(clientName);
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{TRUST_ALL_MANAGER}, null);
            return sslContext;
        } catch (Exception e) {
            throw new ArkException("Failed to create trust-all SSLContext", e);
        }
    }

    private static final X509TrustManager TRUST_ALL_MANAGER = new X509TrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
            // trust all
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
            // trust all
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    };
}
