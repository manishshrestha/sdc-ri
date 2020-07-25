package org.somda.sdc.proto.common;

import org.somda.sdc.proto.model.common.CommonTypes;

public class ProtoConstants {
    public static final String PROTOSDC_NAMESPACE = "https://somda.org/proto-sdc";

    public static CommonTypes.QName GET_SERVICE_QNAME = CommonTypes.QName.newBuilder()
            .setNamespace(PROTOSDC_NAMESPACE).setLocalName("Get").build();
    public static CommonTypes.QName SET_SERVICE_QNAME = CommonTypes.QName.newBuilder()
            .setNamespace(PROTOSDC_NAMESPACE).setLocalName("Set").build();
}
