package org.somda.sdc.dpws;

import com.google.common.util.concurrent.Service;

import java.net.NetworkInterface;
import java.util.Collection;

/**
 * Interface that supplies DPWS core functions.
 * <p>
 * This service is required to be started before any other interaction with the DPWS stack.
 * It is required once per Guice module.
 * <p>
 * {@linkplain DpwsFramework} is responsible to run a multicast UDP queue in order to send and receive discovery messages.
 * Moreover, it prints out a list of network adapters for information purposes.
 * <p>
 * Do not forget to stop the DPWS framework for a graceful shutdown.
 */
public interface DpwsFramework extends Service {

    /**
     * Set the network interface to be used by the framework.
     * <p>
     * <em>This may only be set while the framework isn't running.</em>
     * @param networkInterface a network interface
     * @return the {@linkplain DpwsFramework} instance
     */
    DpwsFramework setNetworkInterface(NetworkInterface networkInterface);

    /**
     * Register a service to attach to the frameworks lifecycle.
     * <p>
     * Starts and shuts down registered services when starting and stopping the framework.
     * <em>Services registered when the framework is running may not be stopped together with the framework,
     * depending on implementation.</em>
     * @param services {@linkplain Service}s
     */
    void registerService(Collection<Service> services);
}
