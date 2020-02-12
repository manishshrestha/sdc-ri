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
}