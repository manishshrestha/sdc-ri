package org.somda.sdc.dpws.device;

/**
 * Configuration tags of the DPWS device side.
 *
 * @see org.somda.sdc.dpws.guice.DefaultDpwsConfigModule
 */
public class DeviceConfig {
    /**
     * Configure to provide an unsecured endpoint.
     * <p>
     * The configuration can be used together with {@link #SECURED_ENDPOINT}.
     * <ul>
     * <li>Data type: {@linkplain Boolean}
     * <li>Use: optional
     * </ul>
     */
    @Deprecated(since = "1.1.0", forRemoval = true)
    public static final String UNSECURED_ENDPOINT = "Dpws.Device.UnsecuredEndpoint";

    /**
     * Configure to provide a secured endpoint.
     * <p>
     * The configuration can be used together with {@link #UNSECURED_ENDPOINT}.
     * <ul>
     * <li>Data type: {@linkplain Boolean}
     * <li>Use: optional
     * </ul>
     */
    @Deprecated(since = "1.1.0", forRemoval = true)
    public static final String SECURED_ENDPOINT = "Dpws.Device.SecuredEndpoint";

    /**
     * Defines the mode that describes how to provide WSDL data.
     * <p>
     * <ul>
     * <li>Data type: {@linkplain org.somda.sdc.dpws.wsdl.WsdlProvisioningMode}
     * <li>Use: optional
     * </ul>
     */
    public static final String WSDL_PROVISIONING_MODE = "Dpws.WsdlProvisioningMode";
}
