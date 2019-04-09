package org.ieee11073.sdc.dpws.soap.wstransfer;

/**
 * WS-Transfer constants.
 */
public class WsTransferConstants {
    public static final String JAXB_CONTEXT_PACKAGE = "org.ieee11073.sdc.dpws.soap.wstransfer.model";

    /**
     * @see <a href="https://www.w3.org/Submission/2006/SUBM-WS-Transfer-20060927/#XML_Namespaces">XML Namespaces</a>
     */
    public static final String NAMESPACE = "http://schemas.xmlsoap.org/ws/2004/09/transfer";

    /**
     * @see <a href="https://www.w3.org/Submission/2006/SUBM-WS-Transfer-20060927/#Get">Get</a>
     */
    public static final String WSA_ACTION_GET = NAMESPACE + "/Get";

    /**
     * @see <a href="https://www.w3.org/Submission/2006/SUBM-WS-Transfer-20060927/#Get">Get</a>
     */
    public static final String WSA_ACTION_GET_RESPONSE = NAMESPACE + "/GetResponse";
}
