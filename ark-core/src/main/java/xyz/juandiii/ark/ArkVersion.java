package xyz.juandiii.ark;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Provides version information resolved from build properties.
 *
 * @author Juan Diego Lopez V.
 */
public final class ArkVersion {

    private static final Logger LOG = Logger.getLogger(ArkVersion.class.getName());

    public static final String NAME = "Ark";
    public static final String VERSION;

    static {
        Properties props = new Properties();
        try (InputStream is = ArkVersion.class.getResourceAsStream("/ark-version.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException ignored) {
        }
        String version = props.getProperty("ark.version");
        if (version == null) {
            LOG.warning("Could not resolve Ark version from ark-version.properties");
            version = "unresolved";
        }
        VERSION = version;
    }

    private ArkVersion() {}
}
