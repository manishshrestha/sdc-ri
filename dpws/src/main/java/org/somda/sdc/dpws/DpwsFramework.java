package org.somda.sdc.dpws;

import com.google.common.util.concurrent.Service;

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
}
