package org.somda.sdc.dpws.soap;

import org.somda.sdc.dpws.DpwsConstants;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingConstants;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryConstants;
import org.somda.sdc.dpws.soap.wseventing.WsEventingConfig;
import org.somda.sdc.dpws.soap.wseventing.WsEventingConstants;
import org.somda.sdc.dpws.soap.wsmetadataexchange.WsMetadataExchangeConstants;
import org.somda.sdc.dpws.soap.wstransfer.WsTransferConstants;

import javax.xml.namespace.QName;

/**
 * SOAP 1.2 constants.
 *
 * @see <a href="https://www.w3.org/TR/2007/REC-soap12-part1-20070427/">SOAP Version 1.2 Part 1: Messaging Framework (Second Edition)</a>
 */
public class SoapConstants {
    /**
     * Package that includes all JAXB generated SOAP objects.
     */
    public static final String JAXB_CONTEXT_PACKAGE = "org.somda.sdc.dpws.soap.model";

    /**
     * SOAP 1.2 namespace.
     *
     * @see <a href="https://www.w3.org/TR/2007/REC-soap12-part1-20070427/#notation">Namespaces</a>
     */
    public static final String NAMESPACE = "http://www.w3.org/2003/05/soap-envelope";

    /**
     * XML Schema instance namespace.
     * <p>
     * Remark: atually, this namespace belongs to a different constant file that describes XML or rather XML Schema
     * constants. Since there are no further constants being used through the project, this constant is defined
     * on the SOAP level, the closest logical layer next to XML here.
     */
    public static final String NAMESPACE_XSI = "http://www.w3.org/2001/XMLSchema-instance";

    /**
     * SOAP fault code "Receiver".
     *
     * @see <a href="https://www.w3.org/TR/2007/REC-soap12-part1-20070427/#faultcodes">SOAP Fault Codes</a>
     */
    public static final QName RECEIVER = new QName(NAMESPACE, "Receiver");

    /**
     * SOAP fault code "Sender".
     *
     * @see <a href="https://www.w3.org/TR/2007/REC-soap12-part1-20070427/#faultcodes">SOAP Fault Codes</a>
     */
    public static final QName SENDER = new QName(NAMESPACE, "Sender");

    /**
     * Default sub-code.
     */
    public static final QName DEFAULT_SUBCODE = new QName(NAMESPACE, "Unknown");

    /**
     * SOAP HTTP binding media type.
     *
     * @see <a href="http://www.ietf.org/rfc/rfc3902.txt">The "application/soap+xml" media type</a>
     */
    public static final String MEDIA_TYPE_SOAP = "application/soap+xml";

    /**
     * WSDL HTTP binding media type.
     *
     * @see <a href="https://www.ietf.org/mail-archive/web/ietf-types/current/msg00287.html">Proposed media type registration: application/wsdl+xml</a>
     */
    public static final String MEDIA_TYPE_WSDL = "text/xml";
    //public static final String MEDIA_TYPE_WSDL = "application/wsdl+xml";

    /**
     * Qualified name of a SOAP 1.2 fault.
     */
    public static final QName FAULT = new QName(NAMESPACE, "Fault");

    /**
     * Definition of namespace prefix mappings relevant to SOAP (including WS-* and OASIS standards).
     */
    public static final String NAMESPACE_PREFIX_MAPPINGS = "{xsi:" + NAMESPACE_XSI + "}" +
            "{wsa:" + WsAddressingConstants.NAMESPACE + "}" +
            "{wse:" + WsEventingConstants.NAMESPACE + "}" +
            "{wsd:" + WsDiscoveryConstants.NAMESPACE + "}" +
            "{wsm:" + WsMetadataExchangeConstants.NAMESPACE + "}" +
            "{wst:" + WsTransferConstants.NAMESPACE + "}" +
            "{dpws:" + DpwsConstants.NAMESPACE + "}" +
            "{s12:" + NAMESPACE + "}";
}
