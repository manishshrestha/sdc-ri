package org.ieee11073.sdc.dpws.client.helper;

import org.ieee11073.sdc.dpws.client.ClientConfig;
import org.ieee11073.sdc.dpws.service.HostingServiceProxy;

/**
 * A simple watchdog implementation.
 */
public interface WatchDog {
    /**
     * Starts a watchdog for a given {@link HostingServiceProxy}.
     * <p>
     * Check {@link ClientConfig#WATCHDOG_PERIOD} to see in which period the watchdog is triggered.
     *
     * @param hostingServiceProxy the hosting service proxy to start the watchdog for.
     */
    void inspect(HostingServiceProxy hostingServiceProxy);
}
