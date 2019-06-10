package org.ieee11073.sdc.dpws;

import com.google.common.util.concurrent.Service;

/**
 * Interface to supply DPWS core functions.
 *
 * This service is required to be started via {@linkplain #startAsync()} before any interaction with
 * and/or {@link org.ieee11073.sdc.dpws.client.Client}.
 *
 * In order to gracefully stop the DPWS framework, please invoke {@linkplain #stopAsync()}.
 */
public interface DpwsFramework extends Service {
}
