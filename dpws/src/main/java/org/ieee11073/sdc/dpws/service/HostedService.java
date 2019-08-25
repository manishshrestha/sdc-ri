package org.ieee11073.sdc.dpws.service;

import org.ieee11073.sdc.dpws.device.WebService;
import org.ieee11073.sdc.dpws.model.HostedServiceType;

import java.io.InputStream;
import java.net.URI;
import java.util.List;

/**
 * POJO-like hosted service definition.
 */
public interface HostedService {
    /**
     * HostedServiceType definition.
     *
     * @return a copy of the internal information.
     */
    HostedServiceType getType();

    /**
     * @return Web Service interceptor where service logic is stored.
     */
    WebService getWebService();

    /**
     * InputStream with WSDL document.
     *
     * @return InputStream object.
     */
    InputStream getWsdlDocument();

    /**
     * Reference to list of URIs that points to WSDL document resource.
     *
     * The user is allowed to alter this list. Returned is not necessarily thread-safe!
     *
     * @return URI objects.
     */
    List<URI> getWsdlLocations();
}