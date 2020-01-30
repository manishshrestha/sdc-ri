package com.example;

import org.somda.sdc.dpws.crypto.CryptoSettings;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Pattern;

public class CustomCryptoSettings implements CryptoSettings {

    private static final String keyStorePath = "crypto/sdcparticipant.jks";
    private static final String trustStorePath = "crypto/root.jks";
    private static final String keyStorePassword = "whatever";
    private static final String trustStorePassword = "whatever";

    @Override
    public Optional<File> getKeyStoreFile() {
        return Optional.empty();
    }

    @Override
    public Optional<InputStream> getKeyStoreStream() {
        return Optional.ofNullable(getClass().getClassLoader().getResourceAsStream(keyStorePath));
    }

    @Override
    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    @Override
    public Optional<File> getTrustStoreFile() {
        return Optional.empty();
    }

    @Override
    public Optional<InputStream> getTrustStoreStream() {
        return Optional.ofNullable(getClass().getClassLoader().getResourceAsStream(trustStorePath));
    }

    @Override
    public String getTrustStorePassword() {
        return trustStorePassword;
    }
}
