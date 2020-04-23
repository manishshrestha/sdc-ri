package org.somda.sdc.dpws.device;

import com.google.inject.Inject;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.UUID;

/**
 * Default device settings if none are injected to a {@linkplain Device}.
 */
public class DefaultDeviceSettings implements DeviceSettings {
    private static final Logger LOG = LogManager.getLogger(DefaultDeviceSettings.class);

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
