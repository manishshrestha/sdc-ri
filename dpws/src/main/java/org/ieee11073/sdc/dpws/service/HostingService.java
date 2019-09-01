package org.ieee11073.sdc.dpws.service;

import org.ieee11073.sdc.dpws.model.ThisDeviceType;
import org.ieee11073.sdc.dpws.model.ThisModelType;
import org.ieee11073.sdc.dpws.soap.interception.Interceptor;
import org.ieee11073.sdc.dpws.service.factory.HostedServiceFactory;

import java.net.URI;
import java.util.List;

/**
 * Hosting service interface.
 *
 * The hosting service depends on WS-Discovery target service information.
 */
public interface HostingService extends Interceptor {
    /**
     * Unique WS-Discovery target service EPR.
     *
     * As defined in dpws:R0004, the URI is a UUID IRI.
     *
     * @see <a href="http://docs.oasis-open.org/ws-dd/dpws/1.1/os/wsdd-dpws-1.1-spec-os.html#_Toc228672089">WS-Addressing</a>
     *
     * @return EPR {@link URI}
     */
    URI getEndpointReferenceAddress();

    /**
     * @return Physical addresses the hosting service is reachable from. Forwards addresses from WS-Discovery XAddr field.
     *
     * @see <a href="http://docs.oasis-open.org/ws-dd/discovery/1.1/os/wsdd-discovery-1.1-spec-os.html#_Toc234231821">Hello</a>
     */
    List<URI> getXAddrs();

    /**
     * @return ThisModel information as defined in DPWS.
     *
     * @see <a href="http://docs.oasis-open.org/ws-dd/dpws/1.1/os/wsdd-dpws-1.1-spec-os.html#_Toc228672093">Characteristics</a>
     */
    ThisModelType getThisModel();

    /**
     * Set ThisModel information as defined in DPWS.
     *
     * @see <a href="http://docs.oasis-open.org/ws-dd/dpws/1.1/os/wsdd-dpws-1.1-spec-os.html#_Toc228672093">Characteristics</a>
     */
    void setThisModel(ThisModelType thisModel);

    /**
     * @return ThisDevice information as defined in DPWS.
     *
     * @see <a href="http://docs.oasis-open.org/ws-dd/dpws/1.1/os/wsdd-dpws-1.1-spec-os.html#_Toc228672093">Characteristics</a>
     */
    ThisDeviceType getThisDevice();

    /**
     * Set ThisDevice information as defined in DPWS.
     *
     * @see <a href="http://docs.oasis-open.org/ws-dd/dpws/1.1/os/wsdd-dpws-1.1-spec-os.html#_Toc228672093">Characteristics</a>
     */
    void setThisDevice(ThisDeviceType thisDevice);

    /**
     * Add hosted service to hosting service.
     *
     * Use {@link HostedServiceFactory} to create suitable {@link HostedService}
     * instances.
     *
     * @see <a href="http://docs.oasis-open.org/ws-dd/dpws/1.1/os/wsdd-dpws-1.1-spec-os.html#_Toc228672094">Hosting</a>
     */
    void addHostedService(HostedService hostedService);

    /**
     * @return a list of all registered hosted services.
     *
     * @see <a href="http://docs.oasis-open.org/ws-dd/dpws/1.1/os/wsdd-dpws-1.1-spec-os.html#_Toc228672094">Hosting</a>
     */
    List<HostedService> getHostedServices();
}
