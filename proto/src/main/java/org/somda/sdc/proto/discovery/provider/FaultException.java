package org.somda.sdc.proto.discovery.provider;

import org.somda.sdc.proto.model.common.CommonTypes;
import org.somda.sdc.proto.model.discovery.DiscoveryMessages;

import javax.xml.namespace.QName;

public class FaultException extends Exception {
    private final DiscoveryMessages.Fault fault;

    public FaultException(QName code, QName subCode, String reason) {
        this.fault = DiscoveryMessages.Fault.newBuilder()
                .setCode(map(code))
                .setSubCode(map(subCode))
                .setReason(reason)
                .build();
    }

    public DiscoveryMessages.Fault getFault() {
        return fault;
    }

    @Override
    public String getMessage() {
        return fault.getReason();
    }

    private CommonTypes.QName map(QName qName) {
        return CommonTypes.QName.newBuilder()
                .setNamespace(qName.getNamespaceURI())
                .setLocalName(qName.getLocalPart())
                .build();
    }
}
