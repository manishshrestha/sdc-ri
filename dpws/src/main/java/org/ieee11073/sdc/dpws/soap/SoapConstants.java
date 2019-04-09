package org.ieee11073.sdc.dpws.soap;

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
    public static final String JAXB_CONTEXT_PACKAGE = "org.ieee11073.sdc.dpws.soap.model";

    /**
     * SOAP 1.2 namespace.
     *
     * @see <a href="https://www.w3.org/TR/2007/REC-soap12-part1-20070427/#notation">Namespaces</a>
     */
    public static final String NAMESPACE = "http://www.w3.org/2003/05/soap-envelope";

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

    public static final QName FAULT = new QName(NAMESPACE, "Fault");
}
