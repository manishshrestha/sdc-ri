package org.ieee11073.sdc.dpws.client;

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
