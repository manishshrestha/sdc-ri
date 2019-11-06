package org.ieee11073.sdc.dpws.crypto;

import com.google.inject.Inject;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.jersey.SslConfigurator;
import org.ieee11073.sdc.common.util.StreamUtil;

import java.io.IOException;

/**
 * Supports generation of server and client SSL configurations.
 * <p>
 * Can either generate default configurations or derive configurations based on {@link CryptoSettings} objects.
 */
public class CryptoConfigurator {
    private final StreamUtil streamUtil;

    @Inject
    CryptoConfigurator(StreamUtil streamUtil) {
        this.streamUtil = streamUtil;
    }

    /**
     * Accepts a {@link CryptoSettings} object and creates an {@linkplain SslConfigurator} object.
     * <p>
     * The {@linkplain SslConfigurator} object can be used, e.g., with Jersey clients.
     *
     * @param cryptoSettings the crypto settings.
     *                       Please note that key store files take precedence over key store streams.
     * @return an SSL configurator matching the given crypto settings.
     */
    public SslConfigurator createSslConfiguratorFromCryptoConfig(CryptoSettings cryptoSettings) {
        final SslConfigurator sslConfig = SslConfigurator.newInstance(false);

        // Configure key store
        sslConfig.keyStorePassword(cryptoSettings.getKeyStorePassword());
        if (cryptoSettings.getKeyStoreFile().isPresent()) {
            sslConfig.keyStoreFile(cryptoSettings.getKeyStoreFile().get().getAbsolutePath());
        } else {
            try {
                sslConfig.keyStoreBytes(streamUtil.getByteArrayFromInputStream(cryptoSettings.getKeyStoreStream()
                        .orElseThrow(() -> new IllegalArgumentException("no stream available"))));
            } catch (IOException e) {
                throw new IllegalArgumentException(
                        String.format("Cryptography activated, but no key store could be read: %s", e.getMessage()));
            }
        }

        // Configure trust store
        sslConfig.trustStorePassword(cryptoSettings.getTrustStorePassword());
        if (cryptoSettings.getTrustStoreFile().isPresent()) {
            sslConfig.trustStoreFile(cryptoSettings.getTrustStoreFile().get().getAbsolutePath());
        } else {
            try {
                sslConfig.trustStoreBytes(streamUtil.getByteArrayFromInputStream(cryptoSettings.getTrustStoreStream()
                        .orElseThrow(() -> new IllegalArgumentException("no stream available"))));
            } catch (IOException e) {
                throw new IllegalArgumentException(
                        String.format("Cryptography activated, but no trust store could be read: %s", e.getMessage()));
            }
        }

        return sslConfig;
    }

    /**
     * Creates a default {@linkplain SslConfigurator} object based on system properties.
     * <p>
     * The {@linkplain SslConfigurator} object can be used, e.g., with Jersey clients.
     *
     * @return an SSL configurator with default crypto settings.
     */
    public SslConfigurator createSslConfiguratorFromSystemProperties() {
        return SslConfigurator.newInstance(true);
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
                sslConfig.setKeyStoreBytes(streamUtil.getByteArrayFromInputStream(cryptoSettings.getKeyStoreStream()
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
                sslConfig.setTrustStoreBytes(streamUtil.getByteArrayFromInputStream(cryptoSettings.getTrustStoreStream()
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
