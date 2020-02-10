package org.somda.sdc.dpws.crypto;

import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;

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

    /**
     * Accepts a {@link CryptoSettings} object and creates an {@linkplain SSLContextConfigurator} object.
     * <p>
     * The {@linkplain SSLContextConfigurator} object can be used, e.g., with Grizzly servers.
     *
     * @param cryptoSettings the crypto settings.
     *                       Please note that key store files take precedence over key store streams.
     * @return configured {@linkplain SSLContextConfigurator} instance.
     */
    public SSLContextConfigurator createSslContextConfiguratorFromCryptoConfig(CryptoSettings cryptoSettings) {
        final SSLContextConfigurator sslConfig = new SSLContextConfigurator(false);

        // Configure key store
        sslConfig.setKeyPass(cryptoSettings.getKeyStorePassword());
        if (cryptoSettings.getKeyStoreFile().isPresent()) {
            sslConfig.setKeyStoreFile(cryptoSettings.getKeyStoreFile().get().getAbsolutePath());
        } else {
            try {
                sslConfig.setKeyStoreBytes(ByteStreams.toByteArray(cryptoSettings.getKeyStoreStream()
                        .orElseThrow(() -> new IllegalArgumentException("no stream available"))));
            } catch (IOException e) {
                throw new IllegalArgumentException(
                        String.format("Cryptography activated, but no key store could be read: %s", e.getMessage()));
            }
        }

        // Configure trust store
        sslConfig.setTrustStorePass(cryptoSettings.getTrustStorePassword());
        if (cryptoSettings.getTrustStoreFile().isPresent()) {
            sslConfig.setTrustStoreFile(cryptoSettings.getTrustStoreFile().get().getAbsolutePath());
        } else {
            try {
                sslConfig.setTrustStoreBytes(ByteStreams.toByteArray(cryptoSettings.getTrustStoreStream()
                        .orElseThrow(() -> new IllegalArgumentException("no stream available"))));
            } catch (IOException e) {
                throw new IllegalArgumentException(
                        String.format("Cryptography activated, but no trust store could be read: %s", e.getMessage()));
            }
        }

        return sslConfig;
    }

    /**
     * Creates a default {@linkplain SSLContextConfigurator} object based on system properties.
     * <p>
     * The {@linkplain SSLContextConfigurator} object can be used, e.g., with Grizzly servers.
     *
     * @return configured {@linkplain SSLContextConfigurator} instance.
     */
    public SSLContextConfigurator createSslContextConfiguratorSystemProperties() {
        return new SSLContextConfigurator(true);
    }
}
