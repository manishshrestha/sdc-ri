package org.somda.sdc.dpws.soap.wsmetadataexchange;

import javax.xml.namespace.QName;

/**
 * WS-MetadataExchange constants.
 *
 * @see <a href="https://www.w3.org/Submission/2008/SUBM-WS-MetadataExchange-20080813/">WS-MetadataExchange specification</a>
 */
public class WsMetadataExchangeConstants {
    public static final String JAXB_CONTEXT_PACKAGE = "org.somda.sdc.dpws.soap.wsmetadataexchange.model";

    /**
     * Resource path to WS-MetadataExchange XML Schema.
     */
    public static final String SCHEMA_PATH = "ws-metadataexchange-schema.xsd";

    public static final String NAMESPACE = "http://schemas.xmlsoap.org/ws/2004/09/mex";
    public static final String WSA_ACTION_GET_METADATA_REQUEST = NAMESPACE + "/GetMetadata/Request";
    public static final String WSA_ACTION_GET_METADATA_RESPONSE = NAMESPACE + "/GetMetadata/Response";
    public static final String DIALECT_WSDL = "http://schemas.xmlsoap.org/wsdl/";

    public static final QName METADATA_REFERENCE = new QName(NAMESPACE, "MetadataReference");
}
