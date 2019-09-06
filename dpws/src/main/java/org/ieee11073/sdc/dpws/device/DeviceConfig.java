package org.ieee11073.sdc.dpws.device;

/**
  * Configuration tags of the DPWS device side.
  */
public class DeviceConfig {
    /**
     * Configure to provide an unsecured endpoint.
     * <p><ul>
     * <li>Data type: {@linkplain Boolean}
     * <li>Use: optional
     * </ul>
     */
    public static final String UNSECURED_ENDPOINT = "Dpws.Device.UnsecuredEndpoint";

    /**
     * Configure to provide a secured endpoint.
     * <p><ul>
     * <li>Data type: {@linkplain Boolean}
     * <li>Use: optional
     * </ul>
     */
    public static final String SECURED_ENDPOINT = "Dpws.Device.SecuredEndpoint";
}
