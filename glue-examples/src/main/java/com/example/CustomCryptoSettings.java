package com.example;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.somda.sdc.dpws.crypto.CryptoSettings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;

public class CustomCryptoSettings implements CryptoSettings {
    private static final Logger LOG = LogManager.getLogger(CustomCryptoSettings.class);

    private static final String DEFAULT_KEYSTORE = "crypto/sdcparticipant.jks";
    private static final String DEFAULT_TRUSTSTORE = "crypto/root.jks";
    private static final String DEFAULT_KEYSTORE_PASSWORD = "whatever";
    private static final String DEFAULT_TRUSTSTORE_PASSWORD = "whatever";

    private File keyStorePath = null;
    private File trustStorePath = null;
    private String keyStorePassword = null;
    private String trustStorePassword = null;

    public CustomCryptoSettings(
            String keyStorePath,
            String trustStorePath,
            String keyStorePassword,
            String trustStorePassword) {
        this.keyStorePath = new File(keyStorePath);
        this.trustStorePath = new File(trustStorePath);
        this.keyStorePassword = keyStorePassword;
        this.trustStorePassword = trustStorePassword;
    }

    public CustomCryptoSettings() {
    }

    @Override
    public Optional<File> getKeyStoreFile() {
        return Optional.empty();
    }

    @Override
    public Optional<InputStream> getKeyStoreStream() {
        if (keyStorePath != null) {
            try {
                return Optional.of(new FileInputStream(keyStorePath.getPath()));
            } catch (FileNotFoundException e) {
                LOG.error("Specified keystore file could not be found", e);
                throw new RuntimeException("Specified keystore file could not be found", e);
            }
        }
        return Optional.ofNullable(getClass().getClassLoader().getResourceAsStream(DEFAULT_KEYSTORE));
    }

    @Override
    public String getKeyStorePassword() {
        return Objects.requireNonNullElse(this.keyStorePassword, DEFAULT_KEYSTORE_PASSWORD);
    }

    @Override
    public Optional<File> getTrustStoreFile() {
        return Optional.empty();
    }

    @Override
    public Optional<InputStream> getTrustStoreStream() {
        if (trustStorePath != null) {
            try {
                return Optional.of(new FileInputStream(trustStorePath.getPath()));
            } catch (FileNotFoundException e) {
                LOG.error("Specified truststore file could not be found", e);
                throw new RuntimeException("Specified truststore file could not be found", e);
            }

        }
        return Optional.ofNullable(getClass().getClassLoader().getResourceAsStream(DEFAULT_TRUSTSTORE));
    }

    @Override
    public String getTrustStorePassword() {
        return Objects.requireNonNullElse(trustStorePassword, DEFAULT_TRUSTSTORE_PASSWORD);
    }
}
