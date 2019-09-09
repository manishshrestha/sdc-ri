package org.ieee11073.sdc.dpws.device;

import org.ieee11073.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Settings used in the setup process of a device.
 */
public interface DeviceSettings {
    /**
     * The unique and persisted endpoint reference (EPR) of the device.
     * 
     * @return the endpoint reference of the device.
     */
    EndpointReferenceType getEndpointReference();

    /**
     * The network interface the device shall bind to.
     *
     * @return the network interface to bind to.
     */
    NetworkInterface getNetworkInterface();
}
