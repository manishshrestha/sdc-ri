package org.somda.sdc.dpws.crypto;

import com.google.inject.Inject;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;

/**
 * Supports generation of server and client SSL configurations.
 * <p>
 * Can either generate default configurations or derive configurations based on {@link CryptoSettings} objects.
 */
public class CryptoConfigurator {

    @Inject
    CryptoConfigurator() {
    }

    /**
     * Accepts a {@link CryptoSettings} object and creates an {@linkplain SSLContext} object.
     * <p>
     *
     * @param cryptoSettings the crypto settings.
     *                       Please note that key store files take precedence over key store streams.
     * @return an SSlContext matching the given crypto settings.
     */
    public SSLContext createSslContextFromCryptoConfig(CryptoSettings cryptoSettings)
            throws KeyStoreException, UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, IOException, KeyManagementException {
        final SSLContextBuilder sslContextBuilder = SSLContexts.custom();

        // key store
        if (cryptoSettings.getKeyStoreFile().isPresent()) {
            sslContextBuilder
                    .loadKeyMaterial(
                            cryptoSettings.getKeyStoreFile().get(),
                            cryptoSettings.getKeyStorePassword().toCharArray(),
                            cryptoSettings.getKeyStorePassword().toCharArray()
                    );
        } else if (cryptoSettings.getKeyStoreStream().isPresent()) {
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(cryptoSettings.getKeyStoreStream().get(), cryptoSettings.getKeyStorePassword().toCharArray());
            sslContextBuilder.loadKeyMaterial(ks, cryptoSettings.getKeyStorePassword().toCharArray());
        } else {
            throw new IOException("Expected key store, but none found");
        }

        // trust store
        if (cryptoSettings.getTrustStoreFile().isPresent()) {
            sslContextBuilder
                    .loadTrustMaterial(
                            cryptoSettings.getTrustStoreFile().get(),
                            cryptoSettings.getTrustStorePassword().toCharArray()
                    );
        } else if (cryptoSettings.getTrustStoreStream().isPresent()) {
            KeyStore ts = KeyStore.getInstance(KeyStore.getDefaultType());
            ts.load(cryptoSettings.getTrustStoreStream().get(), cryptoSettings.getTrustStorePassword().toCharArray());
            sslContextBuilder.loadTrustMaterial(ts, null);
        } else {
            throw new IOException("Expected trust store, but none found");
        }

        return sslContextBuilder.build();
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
