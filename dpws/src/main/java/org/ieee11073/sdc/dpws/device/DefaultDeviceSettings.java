package org.ieee11073.sdc.dpws.device;

import com.google.inject.Inject;
import org.ieee11073.sdc.dpws.soap.SoapUtil;
import org.ieee11073.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * If no device configuration is given, this class will be used as a default.
 */
public class DefaultDeviceSettings implements DeviceSettings {
    private final WsAddressingUtil wsaUtil;
    private final SoapUtil soapUtil;
    private final EndpointReferenceType endpointReference;

    @Inject
    DefaultDeviceSettings(WsAddressingUtil wsaUtil,
                          SoapUtil soapUtil) {
        this.wsaUtil = wsaUtil;
        this.soapUtil = soapUtil;
        this.endpointReference = wsaUtil.createEprWithAddress(soapUtil.createUriFromUuid(UUID.randomUUID()));
    }

    @Override
    public EndpointReferenceType getEndpointReference() {
        return endpointReference;
    }

    @Override
    public List<URI> getHostingServiceBindings() {
        return Collections.singletonList(URI.create("http://localhost:8080"));
    }
}
