package org.somda.sdc.proto.crypto;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.dpws.crypto.CryptoSettings;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Optional;

public class CryptoUtil {
    private static final Logger LOG = LogManager.getLogger(CryptoUtil.class);

    public static Optional<KeyManagerFactory> loadKeyStore(CryptoSettings cryptoSettings) {
        if (cryptoSettings.getKeyStoreStream().isPresent()) {
            try {
                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(cryptoSettings.getKeyStoreStream().get(), cryptoSettings.getKeyStorePassword().toCharArray());

                KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
                keyManagerFactory.init(keyStore, cryptoSettings.getKeyStorePassword().toCharArray());

                return Optional.of(keyManagerFactory);
            } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException | UnrecoverableKeyException e) {
                LOG.error("Could not load keystore", e);
            }
        }
        return Optional.empty();
    }

    public static Optional<TrustManager> loadTrustStore(CryptoSettings cryptoSettings) {
        if (cryptoSettings.getTrustStoreStream().isPresent()) {
            try {
                KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                trustStore.load(cryptoSettings.getTrustStoreStream().get(), cryptoSettings.getTrustStorePassword().toCharArray());

                return Optional.of(new ClientTrustManager(trustStore));
            } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
                LOG.error("Could not load truststore", e);
            }
        }
        return Optional.empty();
    }
}
