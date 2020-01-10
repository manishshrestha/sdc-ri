package org.somda.sdc.mdpws.common;

import javax.xml.namespace.QName;

/**
 * Constants defined by the common package of the MDPWS module.
 */
public class CommonConstants {
    /**
     * JAXB context paths used to let JAXB recognize the MDPWS model.
     */
    public static final String JAXB_CONTEXT_PATH = "org.somda.sdc.mdpws.model";

    /**
     * Definition of the SDC MDPWS target namespace.
     */
    public static final String NAMESPACE = "http://standards.ieee.org/downloads/11073/11073-20702-2016";

    /**
     * Defines the MDPWS device type that is required to identify an MDPWS compliant device during discovery.
     */
    public static final QName MEDICAL_DEVICE_TYPE = new QName(NAMESPACE, "MedicalDevice");
}
