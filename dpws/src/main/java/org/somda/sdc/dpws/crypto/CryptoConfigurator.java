package org.somda.sdc.dpws.crypto;

import com.google.inject.Inject;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Supports generation of server and client SSL configurations.
 * <p>
 * Can either generate default configurations or derive configurations based on {@link CryptoSettings} objects.
 */
public class CryptoConfigurator {
    private static final Logger LOG = LogManager.getLogger(CryptoConfigurator.class);

    private CryptoSettings knownSettingsForSslContext = null;
    private SSLContextBuilder builderforKnownSettings = null;

    private CryptoSettings knownSettingsForCertificates = null;
    private List<X509Certificate> certificatesForKnownSettings = null;

    @Inject
    CryptoConfigurator() {
    }

    /**
     * Accepts a {@link CryptoSettings} object and creates an {@linkplain SSLContext} object.
     * <p>
     *
     * @param cryptoSettings the crypto settings.
     *
     * @return an SSlContext matching the given crypto settings.
     */
    public SSLContext createSslContextFromCryptoConfig(CryptoSettings cryptoSettings)
            throws KeyStoreException, UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException,
            IOException, KeyManagementException {
        if (cryptoSettings == knownSettingsForSslContext) {
            return builderforKnownSettings.build();
        }

        final SSLContextBuilder sslContextBuilder = SSLContexts.custom();

        // key store
        if (cryptoSettings.getKeyStoreStream().isPresent()) {
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(cryptoSettings.getKeyStoreStream().get(), cryptoSettings.getKeyStorePassword().toCharArray());
            sslContextBuilder.loadKeyMaterial(ks, cryptoSettings.getKeyStorePassword().toCharArray());
        } else {
            throw new IOException("Expected key store, but none found");
        }

        // trust store
        if (cryptoSettings.getTrustStoreStream().isPresent()) {
            KeyStore ts = KeyStore.getInstance(KeyStore.getDefaultType());
            ts.load(cryptoSettings.getTrustStoreStream().get(), cryptoSettings.getTrustStorePassword().toCharArray());
            sslContextBuilder.loadTrustMaterial(ts, null);
        } else {
            throw new IOException("Expected trust store, but none found");
        }

        knownSettingsForSslContext = cryptoSettings;
        builderforKnownSettings = sslContextBuilder;
        return sslContextBuilder.build();
    }

    /**
     * Accepts a {@link CryptoSettings} object and extracts all certificates from the keystore.
     * <p>
     *
     * @param cryptoSettings the crypto settings.
     *                       Please note that key store files take precedence over key store streams.
     * @return a list of all X509 certificates from the keystore or an empty list.
     */
    public List<X509Certificate> getCertificates(@Nullable CryptoSettings cryptoSettings) {
        if (cryptoSettings == knownSettingsForCertificates) {
            return certificatesForKnownSettings;
        }

        List<X509Certificate> certificates = new ArrayList<>();
        if (cryptoSettings == null) return certificates;
        try {
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            if (cryptoSettings.getKeyStoreStream().isPresent()) {
                ks.load(cryptoSettings.getKeyStoreStream().get(), cryptoSettings.getKeyStorePassword().toCharArray());
            } else {
                return certificates;
            }
            var aliases = ks.aliases().asIterator();
            while (aliases.hasNext()) {
                var cert = ks.getCertificate(aliases.next());
                if (cert instanceof X509Certificate) {
                    certificates.add((X509Certificate) cert);
                }
            }
        } catch (CertificateException | KeyStoreException | IOException | NoSuchAlgorithmException e) {
            LOG.error(String.format("Error retrieving certificates from keystore %s", e));
        }

        knownSettingsForCertificates = cryptoSettings;
        certificatesForKnownSettings = Collections.unmodifiableList(certificates);

        return certificates;
    }

    /**
     * Creates a default {@linkplain SSLContext} object based on system properties.
     *
     * @return an SSLContext with default crypto settings.
     */
    public SSLContext createSslContextFromSystemProperties() {
        return SSLContexts.createSystemDefault();
    }

}
