package org.somda.sdc.dpws.soap.wsaddressing;

import javax.xml.namespace.QName;

/**
 * WS-Addressing constants.
 *
 * @see <a href="https://www.w3.org/TR/2006/REC-ws-addr-core-20060509/">Web Services Addressing 1.0 - Core</a>
 * @see <a href="https://www.w3.org/TR/2006/REC-ws-addr-soap-20060509/">Web Services Addressing 1.0 - SOAP Binding</a>
 */
public class WsAddressingConstants {
    /**
     * Package that includes all JAXB generated WS-Addressing objects.
     */
    public static final String JAXB_CONTEXT_PACKAGE = "org.somda.sdc.dpws.soap.wsaddressing.model";

    /**
     * WS-Addressing 1.0 namespace.
     *
     * @see <a href="https://www.w3.org/TR/2006/REC-ws-addr-core-20060509/#namespaces">Namespaces</a>
     */
    public static final String NAMESPACE = "http://www.w3.org/2005/08/addressing";

    /**
     * WS-Addressing wsa:Action header element.
     *
     * @see <a href="https://www.w3.org/TR/2006/REC-ws-addr-core-20060509/#msgaddrprops">Message Addressing Properties</a>
     */
    public static final QName ACTION = new QName(NAMESPACE, "Action");

    /**
     * WS-Addressing wsa:MessageID header element.
     *
     * @see <a href="https://www.w3.org/TR/2006/REC-ws-addr-core-20060509/#msgaddrprops">Message Addressing Properties</a>
     */
    public static final QName MESSAGE_ID = new QName(NAMESPACE, "MessageID");

    /**
     * WS-Addressing wsa:ReplyTo header element.
     *
     * @see <a href="https://www.w3.org/TR/2006/REC-ws-addr-core-20060509/#msgaddrprops">Message Addressing Properties</a>
     */
    public static final QName RELATES_TO = new QName(NAMESPACE, "RelatesTo");

    /**
     * WS-Addressing wsa:To header element.
     *
     * @see <a href="https://www.w3.org/TR/2006/REC-ws-addr-core-20060509/#msgaddrprops">Message Addressing Properties</a>
     */
    public static final QName TO = new QName(NAMESPACE, "To");

    /**
     * ActionNotSupported fault QName.
     *
     * @see <a href="https://www.w3.org/TR/2006/REC-ws-addr-soap-20060509/#actionfault">Action Not Supported</a>
     */
    public static final QName ACTION_NOT_SUPPORTED = new QName(NAMESPACE, "ActionNotSupported");

    /**
     * MessageAddressingHeaderRequired fault QName.
     *
     * @see <a href="https://www.w3.org/TR/2006/REC-ws-addr-soap-20060509/#missingmapfault">Message Addressing Header Required</a>
     */
    public static final QName MESSAGE_ADDRESSING_HEADER_REQUIRED = new QName(NAMESPACE, "MessageAddressingHeaderRequired");

    /**
     * WS-Addressing wsa:Action for faults.
     *
     * @see <a href="https://www.w3.org/TR/2006/REC-ws-addr-soap-20060509/#faults">Faults</a>
     */
    public static final String FAULT_ACTION = NAMESPACE + "/fault";

    /**
     * WS-Addressing anonymous wsa:To/wsa:ReplyTo endpoint address.
     *
     * @see <a href="https://www.w3.org/TR/2006/REC-ws-addr-soap-20060509/#anonaddress">Use of Anonymous Address in SOAP Response Endpoints</a>
     */
    public static final String ANONYMOUS = NAMESPACE + "/anonymous";
}