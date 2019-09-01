package org.ieee11073.sdc.dpws.service;

import org.ieee11073.sdc.dpws.model.HostedServiceType;
import org.ieee11073.sdc.dpws.soap.RequestResponseClient;

import java.net.URI;

/**
 * Proxy interface for hosted services.
 */
public interface HostedServiceProxy extends RequestResponseClient {
    /**
     * HostedServiceType definition.
     *
     * @return a copy of the data received from the network.
     */
    HostedServiceType getType();

    /**
     * Request response client used to send request messages and receive their response.
     *
     * Hint: the {@linkplain HostingServiceProxy} itself implements {@link RequestResponseClient}, which is the same
     * as using the return value of this function.
     *
     * @return the {@link RequestResponseClient} instance
     */
    RequestResponseClient getRequestResponseClient();

    /**
     * Event sink that can be used to subscribe and manage subscriptions of the hosted service.
     *
     * Attention: the event sink does only work if the underlying hosted service acts as an event source. This has to
     * be verified by the user in advance, otherwise calls on the return value will end up in SOAP faults.

     * @return the {@link EventSinkAccess} instance
     */
    EventSinkAccess getEventSinkAccess();

    /**
     * Return the physical address that is actively being used to send requests.
     *
     * A hosted service can have different physical addresses in order to be accessible. The one that is returned
     * with this function is the one that was used to initially resolve metadata (GetMetadata request).
     *
     * @return the currently active EPR address
     */
    URI getActiveEprAddress();
}
