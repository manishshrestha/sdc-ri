package org.ieee11073.sdc.dpws.device;

import org.ieee11073.sdc.dpws.crypto.CryptoSettings;

public class DeviceConfig {
    /**
     * Used to retrieve SSL configuration.
     *
     * - Data type: {@link CryptoSettings}
     * - Use: optional
     */
    public static final String CRYPTO_SETTINGS = "Dpws.Device.CryptoSettings";
}
