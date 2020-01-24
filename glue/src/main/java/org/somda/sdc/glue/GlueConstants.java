package org.somda.sdc.glue;

import org.ietf.jgss.Oid;
import org.somda.sdc.glue.common.ParticipantKeyPurposeMapper;

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
     * Resource path to BICEPS XML Schemas.
     */
    public static final String SCHEMA_PATH = "ExtensionPoint.xsd:BICEPS_ParticipantModel.xsd:BICEPS_MessageModel.xsd";

    /**
     * Key purpose dot-notated OID that expresses compliance with all mandatory requirements for an SDC service provider.
     */
    public static final String OID_KEY_PURPOSE_SDC_SERVICE_PROVIDER = "1.2.840.10004.20701.1.1";

    /**
     * Key purpose dot-notated OID that expresses compliance with all mandatory requirements for an SDC service consumer.
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

    static {
        SCOPE_SDC_PROVIDER = null;
        try {
            // This assignment should never throw unless OID_KEY_PURPOSE_SDC_SERVICE_PROVIDER is modified
            // to something malformed. A separate unit test covers this (GlueConstantsTest::staticInitialization())
            SCOPE_SDC_PROVIDER = ParticipantKeyPurposeMapper.fromOid(new Oid(OID_KEY_PURPOSE_SDC_SERVICE_PROVIDER));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Definition of the SDC participant discovery scope.
     * <p>
     * This scope, encoded in accordance with SDC Glue clause 9.3, claims conformance with IEEE 11073-20701, published 2018.
     */
    public static URI SCOPE_SDC_PROVIDER;
}
