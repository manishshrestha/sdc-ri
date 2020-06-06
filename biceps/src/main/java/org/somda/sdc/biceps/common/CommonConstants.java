package org.somda.sdc.biceps.common;

import javax.xml.namespace.QName;

/**
 * Common BICEPS constants.
 */
public class CommonConstants {
    /**
     * Namespace string of the BICEPS Extension Model.
     */
    public static final String NAMESPACE_EXTENSION =
            "http://standards.ieee.org/downloads/11073/11073-10207-2017/extension";

    /**
     * Prefix used for the namespace of the BICEPS Extension Model.
     */
    public static final String NAMESPACE_EXTENSION_PREFIX = "ext";

    /**
     * Namespace string of the BICEPS Participant Model.
     */
    public static final String NAMESPACE_PARTICIPANT =
            "http://standards.ieee.org/downloads/11073/11073-10207-2017/participant";

    /**
     * Prefix used for the namespace of the BICEPS Participant Model.
     */
    public static final String NAMESPACE_PARTICIPANT_PREFIX = "pm";

    /**
     * Namespace string of the BICEPS Message Model.
     */
    public static final String NAMESPACE_MESSAGE = "http://standards.ieee.org/downloads/11073/11073-10207-2017/message";

    /**
     * Prefix used for the namespace of the BICEPS Message Model.
     */
    public static final String NAMESPACE_MESSAGE_PREFIX = "msg";

    /**
     * Designates the qualified name for MustUnderstand attributes attached to BICEPS extensions.
     */
    public static final QName QNAME_MUST_UNDERSTAND_ATTRIBUTE = new QName(NAMESPACE_EXTENSION, "MustUnderstand");

}
