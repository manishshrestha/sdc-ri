package org.ieee11073.sdc.dpws.client.helper;

import org.ieee11073.sdc.dpws.client.ClientConfig;
import org.ieee11073.sdc.dpws.service.HostingServiceProxy;

/**
 * Watchdog API.
 */
public interface WatchDog {
    /**
     * Start watchdog for given {@link HostingServiceProxy}.
     *
     * See {@link ClientConfig#WATCHDOG_PERIOD} to define in which period the watchdog is
     * triggered.
     */
    void inspect(HostingServiceProxy hostingServiceProxy);
}
