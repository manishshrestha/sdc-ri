package org.somda.sdc.glue;

import java.net.URI;

/**
 * Any constants relevant to SDC Glue.
 */
public class GlueConstants {
    private static String URI_SCHEME_OID = "urn:oid:";

    /**
     * JAXB context paths used to let JAXB recognize the BICEPS model.
     */
    public static final String JAXB_CONTEXT_PATH = "org.somda.sdc.biceps.model.extension:" +
            "org.somda.sdc.biceps.model.participant:" +
            "org.somda.sdc.biceps.model.message";

    /**
     * Key purpose OID that expresses compliance with all mandatory requirements for an SDC service provider.
     */
    public static final String OID_KEY_PURPOSE_SDC_SERVICE_PROVIDER = "1.2.840.10004.20701.1.1";

    /**
     * Key purpose OID that expresses compliance with all mandatory requirements for an SDC service consumer.
     */
    public static final String OID_KEY_PURPOSE_SDC_SERVICE_CONSUMER = "1.2.840.10004.20701.1.2";

    /**
     * Key purpose OID as URI an SDC service provider.
     *
     * @see #OID_KEY_PURPOSE_SDC_SERVICE_PROVIDER
     */
    public static final URI URI_KEY_PURPOSE_SDC_SERVICE_PROVIDER = URI.create(URI_SCHEME_OID + OID_KEY_PURPOSE_SDC_SERVICE_PROVIDER);

    /**
     * Key purpose OID as URI an SDC service consumer.
     *
     * @see #OID_KEY_PURPOSE_SDC_SERVICE_CONSUMER
     */
    public static final URI URI_KEY_PURPOSE_SDC_SERVICE_CONSUMER = URI.create(URI_SCHEME_OID + OID_KEY_PURPOSE_SDC_SERVICE_CONSUMER);

    /**
     * Definition of the SDC participant discovery scope.
     * todo DGr check if SCOPE_SDC_PROVIDER is the right term to use
     */
    public static final URI SCOPE_SDC_PROVIDER = URI.create("sdc.mds.pkp:" + OID_KEY_PURPOSE_SDC_SERVICE_PROVIDER);


}
