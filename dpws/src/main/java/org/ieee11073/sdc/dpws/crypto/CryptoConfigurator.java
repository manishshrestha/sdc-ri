package org.ieee11073.sdc.dpws.crypto;

import com.google.inject.Inject;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.jersey.SslConfigurator;
import org.ieee11073.sdc.common.helper.StreamUtil;

import java.io.IOException;

/**
 * Supports generation of server and client SSL configuration.
 *
 * Can either generate default configurations or derive configurations based on {@link CryptoSettings} objects.
 *
 */
public class CryptoConfigurator {
    private final StreamUtil streamUtil;

    @Inject
    CryptoConfigurator(StreamUtil streamUtil) {
        this.streamUtil = streamUtil;
    }

    /**
     * Accept a {@link CryptoSettings} object and create an {@linkplain SslConfigurator} object to be used, e.g., with
     * Jersey clients.
     *
     * @param cryptoSettings Key store file takes precedence over key store stream.
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
     * Create a default {@linkplain SslConfigurator} object based on system properties to be used, e.g., with Jersey
     * clients.
     */
    public SslConfigurator createSslConfiguratorFromSystemProperties() {
        return SslConfigurator.newInstance(true);
    }

    /**
     * Accept a {@link CryptoSettings} object and create an {@linkplain SSLContextConfigurator} object to be used, e.g.,
     * with Grizzly servers.
     *
     * @param cryptoSettings Key store file takes precedence over key store stream.
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
     * Create a default {@linkplain SSLContextConfigurator} object based on system properties to be used, e.g., with
     * Grizzly servers.
     */
    public SSLContextConfigurator createSslContextConfiguratorSystemProperties() {
        return new SSLContextConfigurator(true);
    }
}
