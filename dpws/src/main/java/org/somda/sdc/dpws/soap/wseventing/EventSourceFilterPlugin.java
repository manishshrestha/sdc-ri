package org.somda.sdc.dpws.soap.wseventing;

import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.interception.RequestResponseObject;

/**
 * Plugin that handles an event source's incoming subscription requests for custom filter dialect subscribe methods.
 */
public interface EventSourceFilterPlugin {
    /**
     * A subscribe method for WS-Eventing subscriptions with custom filter dialect.
     *
     * @param requestResponseObject the request response object that contains the subscribe request data
     * @throws SoapFaultException if something went wrong during processing.
     */
    void subscribe(RequestResponseObject requestResponseObject) throws SoapFaultException;
}
