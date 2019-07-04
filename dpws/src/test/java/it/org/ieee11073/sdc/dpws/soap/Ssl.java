package it.org.ieee11073.sdc.dpws.soap;

import it.org.ieee11073.sdc.dpws.SslMetadata;
import org.ieee11073.sdc.dpws.crypto.CryptoSettings;

import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Simple {@link CryptoSettings} generator for integration tests using HTTPS.
 */
public class Ssl {
    public static CryptoSettings setup() throws IOException {
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        final SslMetadata sslMetadata = new SslMetadata();
        return new CryptoSettings() {
            @Override
            public Optional<File> getKeyStoreFile() {
                return Optional.of(new File(sslMetadata.getKeyStoreFile()));
            }

            @Override
            public Optional<InputStream> getKeyStoreStream() {
                return Optional.empty();
            }

            @Override
            public String getKeyStorePassword() {
                return sslMetadata.getKeyStorePassword();
            }

            @Override
            public Optional<File> getTrustStoreFile() {
                return Optional.of(new File(sslMetadata.getTrustStoreFile()));
            }

            @Override
            public Optional<InputStream> getTrustStoreStream() {
                return Optional.empty();
            }

            @Override
            public String getTrustStorePassword() {
                return sslMetadata.getTrustStorePassword();
            }
        };

        // Info: properties to set for global SSL configuration
        // Not recommended as client and device config may differ, but HTTP server and client access the same
        // system properties.
        // System.setProperty(SslConfigurator.TRUST_STORE_FILE, sslMetadata.getTrustStoreFile());
        // System.setProperty(SslConfigurator.KEY_STORE_FILE, sslMetadata.getKeyStoreFile());
        // System.setProperty(SslConfigurator.TRUST_STORE_PASSWORD, sslMetadata.getTrustStorePassword());
        // System.setProperty(SslConfigurator.KEY_STORE_PASSWORD, sslMetadata.getKeyStorePassword());
    }
}
