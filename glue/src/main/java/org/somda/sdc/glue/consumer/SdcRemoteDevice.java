package org.somda.sdc.glue.consumer;

/**
 * SDC consumer device interface.
 * <p>
 * The purpose of {@linkplain SdcRemoteDevice} is to receive SDC data from the network.
 */
public interface SdcRemoteDevice {
    void registerWatchdogObserver(WatchdogObserver watchdogObserver);
    void unregisterWatchdogObserver(WatchdogObserver watchdogObserver);
}
