package org.somda.sdc.common;

/**
 * Common constants.
 */
public class CommonConfig {

    /**
     * Constant used to configure the instance identifier used in log messages throughout the injector.
     *
     * <ul>
     * <li>Data type: {@linkplain String}
     * <li>Use: optional
     * </ul>
     */
    public static final String INSTANCE_IDENTIFIER = "Common.InstanceIdentifier";

    /**
     * JAXB context paths used to let JAXB recognize the BICEPS model.
     */
    public static final String BICEPS_JAXB_CONTEXT_PATH = "org.somda.sdc.biceps.model.extension:" +
            "org.somda.sdc.biceps.model.participant:" +
            "org.somda.sdc.biceps.model.message";

    /**
     * JAXB context paths used to let JAXB recognize the DPWS model.
     */
    public static final String DPWS_JAXB_CONTEXT_PATH = "org.somda.sdc.dpws.soap.model:" +
            "org.somda.sdc.dpws.model:" +
            "org.somda.sdc.dpws.soap.wsaddressing.model:" +
            "org.somda.sdc.dpws.soap.wsdiscovery.model:" +
            "org.somda.sdc.dpws.soap.wseventing.model:" +
            "org.somda.sdc.dpws.soap.wstransfer.model:" +
            "org.somda.sdc.dpws.soap.wsmetadataexchange.model:" +
            "org.somda.sdc.dpws.wsdl.model";

}
