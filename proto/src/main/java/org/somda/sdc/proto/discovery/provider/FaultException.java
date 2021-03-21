package org.somda.sdc.proto.discovery.provider;

import org.somda.protosdc.proto.model.common.CommonTypes;
import org.somda.protosdc.proto.model.common.LocalizedString;
import org.somda.protosdc.proto.model.discovery.DiscoveryMessages;
import org.somda.protosdc.proto.model.discovery.Fault;

import javax.xml.namespace.QName;

public class FaultException extends Exception {
    private final Fault fault;

    public FaultException(QName code, QName subCode, String reason) {
        this.fault = Fault.newBuilder()
                .setCode(map(code))
                .setSubCode(map(subCode))
                .setReason(LocalizedString.newBuilder().setValue(reason).build())
                .build();
    }

    public Fault getFault() {
        return fault;
    }

    @Override
    public String getMessage() {
        return fault.getReason().getValue();
    }

    private org.somda.protosdc.proto.model.common.QName map(QName qName) {
        return org.somda.protosdc.proto.model.common.QName.newBuilder()
                .setNamespace(qName.getNamespaceURI())
                .setLocalName(qName.getLocalPart())
                .build();
    }
}
