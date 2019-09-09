package org.ieee11073.sdc.dpws.device;

import com.google.inject.Inject;
import org.ieee11073.sdc.dpws.soap.SoapUtil;
import org.ieee11073.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * If no device configuration is given, this class will be used as a default.
 */
public class DefaultDeviceSettings implements DeviceSettings {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultDeviceSettings.class);

    private final EndpointReferenceType endpointReference;

    @Inject
    DefaultDeviceSettings(WsAddressingUtil wsaUtil,
                          SoapUtil soapUtil) {
        this.endpointReference = wsaUtil.createEprWithAddress(soapUtil.createUriFromUuid(UUID.randomUUID()));
    }

    @Override
    public EndpointReferenceType getEndpointReference() {
        return endpointReference;
    }

    @Override
    public NetworkInterface getNetworkInterface() {
        try {
            return NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress());
        } catch (Exception e) {
            LOG.warn("No default network interface was resolvable:", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
