package org.ieee11073.sdc.dpws.soap.factory;

import com.google.inject.assistedinject.Assisted;
import org.ieee11073.sdc.dpws.soap.RequestResponseClient;
import org.ieee11073.sdc.dpws.soap.interception.RequestResponseCallback;

/**
 * Factory to create request-response clients.
 */
public interface RequestResponseClientFactory {
    /**
     * @param callback Callback that shall be invoked after all interceptors on request-direction are visited and a
     *                 response is required from requested server.
     * @return a new {@link RequestResponseClient}
     */
    RequestResponseClient createRequestResponseClient(@Assisted RequestResponseCallback callback);
}
