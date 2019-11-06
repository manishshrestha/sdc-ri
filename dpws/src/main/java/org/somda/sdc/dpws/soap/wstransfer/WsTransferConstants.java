package org.somda.sdc.dpws.soap.wstransfer;

/**
 * WS-Transfer constants.
 *
 * @see <a href="https://www.w3.org/Submission/2006/SUBM-WS-Transfer-20060927/">WS-Transfer specification</a>
 */
public class WsTransferConstants {
    public static final String JAXB_CONTEXT_PACKAGE = "org.somda.sdc.dpws.soap.wstransfer.model";

    /**
     * WS-Transfer namespace.
     *
     * @see <a href="https://www.w3.org/Submission/2006/SUBM-WS-Transfer-20060927/#XML_Namespaces">XML Namespaces</a>
     */
    public static final String NAMESPACE = "http://schemas.xmlsoap.org/ws/2004/09/transfer";

    /**
     * Get request action URI.
     *
     * @see <a href="https://www.w3.org/Submission/2006/SUBM-WS-Transfer-20060927/#Get">Get</a>
     */
    public static final String WSA_ACTION_GET = NAMESPACE + "/Get";

    /**
     * Get response action URI.
     *
     * @see <a href="https://www.w3.org/Submission/2006/SUBM-WS-Transfer-20060927/#Get">Get</a>
     */
    public static final String WSA_ACTION_GET_RESPONSE = NAMESPACE + "/GetResponse";
}
