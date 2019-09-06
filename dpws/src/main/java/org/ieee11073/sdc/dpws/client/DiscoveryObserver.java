package org.ieee11073.sdc.dpws.client;

import org.ieee11073.sdc.dpws.client.event.DeviceEnteredMessage;
import org.ieee11073.sdc.dpws.client.event.DeviceLeftMessage;
import org.ieee11073.sdc.dpws.client.event.DeviceProbeTimeoutMessage;
import org.ieee11073.sdc.dpws.client.event.ProbedDeviceFoundMessage;

/**
 * Indicate class as a discovery observer.
 *
 * Annotate method with {@link com.google.common.eventbus.Subscribe} to
 *
 * - {@link DeviceEnteredMessage}
 * - {@link DeviceLeftMessage}
 * - {@link ProbedDeviceFoundMessage}
 * - {@link DeviceProbeTimeoutMessage}
 */
public interface DiscoveryObserver {
}
