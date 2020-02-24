package org.somda.sdc.dpws.crypto;

/**
 * Configuration keys for cryptographic components.
 *
 * @see org.somda.sdc.dpws.guice.DefaultDpwsConfigModule
 */
public class CryptoConfig {
    /**
     * Used to retrieve SSL configuration.
     * <ul>
     * <li>Data type: {@link CryptoSettings}
     * <li>Use: optional
     * </ul>
     */
    public static final String CRYPTO_SETTINGS = "Dpws.Crypto.Settings";

    /**
     * Protocols enabled for secure communication, support may depend on used libraries.
     *
     * <ul>
     * <li>Data type: {@linkplain String[]}
     * <li>Use: optional
     * </ul>
     */
    public static final String CRYPTO_TLS_ENABLED_VERSIONS = "Dpws.Crypto.TlsEnabledVersions";

}
