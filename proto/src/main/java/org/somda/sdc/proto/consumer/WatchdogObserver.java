package org.somda.sdc.proto.consumer;

import org.somda.sdc.glue.consumer.SdcRemoteDevice;

/**
 * Listens to a Watchdog and receives information if something goes wrong.
 * <p>
 * In order to receive error messages that relate to the watchdog, use {@link com.google.common.eventbus.Subscribe}
 * with a {@link org.somda.sdc.glue.consumer.event.WatchdogMessage} object and subscribe at an {@link SdcRemoteDevice}.
 */
public interface WatchdogObserver {
}
