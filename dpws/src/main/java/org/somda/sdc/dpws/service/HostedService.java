package org.ieee11073.sdc.dpws.service;

import org.ieee11073.sdc.dpws.device.WebService;
import org.ieee11073.sdc.dpws.model.HostedServiceType;

import java.io.InputStream;
import java.net.URI;
import java.util.List;

/**
 * Hosted service information of a device.
 */
public interface HostedService {
    /**
     * Gets the hosted service metadata requestable via WS-TransferGet.
     *
     * @return a copy of the hosted service metadata to be sent over the network.
     */
    HostedServiceType getType();

    /**
     * Gets the interceptor that is responsible to process incoming network requests.
     *
     * @return the Web Service interceptor where service logic is stored.
     */
    WebService getWebService();

    /**
     * Provision of an input stream with WSDL document data.
     *
     * @return input stream with WSDL data used to respond to WS-MetadataExchange requests.
     */
    InputStream getWsdlDocument();

    /**
     * Provision of a list of URIs that points to WSDL document resources.
     * <p>
     * todo DGr the user is allowed to alter this list - might be a design flaw that should be addressed some time.
     *
     * @return URIs that point to WSDL documents, preferably exactly one reference.
     */
    List<URI> getWsdlLocations();
}