package org.somda.sdc.glue.consumer;

import com.google.common.util.concurrent.ListenableFuture;
import org.somda.sdc.dpws.service.HostingServiceProxy;

import java.net.URI;
import java.util.Collection;
import java.util.Optional;

/**
 * Central starting point to gain access to remote SDC devices.
 */
public interface SdcRemoteDevicesConnector {
    /**
     * Tries to establish an SDC client connection to the given hosting service proxy (i.e., remote SDC device).
     * <p>
     * The client connection will pass only if the host is reachable and provides the mandatory BICEPS Get service.
     * After this function is triggered, the following process is conducted:
     * <ol>
     * <li>If there is at least one action in the set of available actions, then the connector subscribes to those actions.
     * <li>The connector starts a watchdog and either uses WS-Eventing Renew requests in case of an existing subscription
     * or DirectedProbe requests in case of no existing subscriptions in order to keep track of the connection to the remote node.
     * <li>The connector collects and buffers all incoming reports.
     * <li>The MDIB is requested from the remote node.
     * <li>Reports are applied on the MDIB.
     * <li>An {@link SdcRemoteDevice} is supplied.
     * </ol>
     * <p>
     * Connections can be triggered only once per device per time. As reports are applied on the received MDIB there
     * are no gaps as long as every subscription has been subscribed during connection.
     *
     * @param hostingServiceProxy  the hosted service to connect to
     * @param connectConfiguration Options for the connection process.
     * @return a future that contains the {@link SdcRemoteDevice} if connection succeeds. It will immediately return if
     * there is an existing connection already.
     */
    ListenableFuture<SdcRemoteDevice> connect(HostingServiceProxy hostingServiceProxy,
                                              ConnectConfiguration connectConfiguration);

    /**
     * Disconnects a device.
     * <p>
     * This function is non-blocking.
     * Right after it returns the disconnected device can be re-connected.
     *
     * @param eprAddress the endpoint reference address of the remote device to disconnect.
     */
    void disconnect(URI eprAddress);

    /**
     * Gets a copy of all connected devices at a certain point in time.
     *
     * @return a collection of all connected devices. Please note that this creates a copy on every call.
     */
    Collection<SdcRemoteDevice> getConnectedDevices();

    /**
     * Gets a connected device.
     *
     * @param eprAddress the endpoint reference address of the remote device to get.
     * @return an {@link SdcRemoteDevice} instance if connected, otherwise {@link Optional#empty()}.
     */
    Optional<SdcRemoteDevice> getConnectedDevice(URI eprAddress);
}
