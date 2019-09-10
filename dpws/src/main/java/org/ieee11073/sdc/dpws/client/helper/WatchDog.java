package org.ieee11073.sdc.dpws.client.helper;

import org.ieee11073.sdc.dpws.client.ClientConfig;
import org.ieee11073.sdc.dpws.service.HostingServiceProxy;

/**
 * Watchdog API.
 */
public interface WatchDog {
    /**
     * Starts watchdog for given {@link HostingServiceProxy}.
     * <p>
     * See {@link ClientConfig#WATCHDOG_PERIOD} to define in which period the watchdog is triggered.
     *
     * @param hostingServiceProxy the hosting service proxy to start the watchdog for.
     */
    void inspect(HostingServiceProxy hostingServiceProxy);
}
