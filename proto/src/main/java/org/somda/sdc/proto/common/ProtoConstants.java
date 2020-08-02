package org.somda.sdc.proto.common;

import org.somda.sdc.proto.model.common.CommonTypes;
import org.somda.sdc.proto.model.common.QName;

public class ProtoConstants {
    public static final String PROTOSDC_NAMESPACE = "https://somda.org/proto-sdc";

    public static QName GET_SERVICE_QNAME = QName.newBuilder()
            .setNamespace(PROTOSDC_NAMESPACE).setLocalName("Get").build();
    public static QName SET_SERVICE_QNAME = QName.newBuilder()
            .setNamespace(PROTOSDC_NAMESPACE).setLocalName("Set").build();
    public static QName MDIB_REPORTING_SERVICE_QNAME = QName.newBuilder()
            .setNamespace(PROTOSDC_NAMESPACE).setLocalName("MdibReporting").build();
}
