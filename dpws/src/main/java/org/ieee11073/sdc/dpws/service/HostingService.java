package org.ieee11073.sdc.dpws.service;

import org.ieee11073.sdc.dpws.model.ThisDeviceType;
import org.ieee11073.sdc.dpws.model.ThisModelType;
import org.ieee11073.sdc.dpws.service.factory.HostedServiceFactory;
import org.ieee11073.sdc.dpws.soap.interception.Interceptor;

import java.net.URI;
import java.util.List;

/**
 * Hosting service interface.
 * <p>
 * The hosting service depends on WS-Discovery target service information.
 */
public interface HostingService extends Interceptor {
    /**
     * Gets the unique WS-Discovery target service EPR.
     * <p>
     * As defined in dpws:R0004, the URI is a UUID IRI.
     *
     * @return the EPR {@link URI}.
     * @see <a href="http://docs.oasis-open.org/ws-dd/dpws/1.1/os/wsdd-dpws-1.1-spec-os.html#_Toc228672089">WS-Addressing</a>
     */
    URI getEndpointReferenceAddress();

    /**
     * Gets the XAddrs list of the hosting service.
     *
     * @return Physical addresses the hosting service is reachable from. Forwards addresses from WS-Discovery XAddr field.
     * @see <a href="http://docs.oasis-open.org/ws-dd/discovery/1.1/os/wsdd-discovery-1.1-spec-os.html#_Toc234231821">Hello</a>
     */
    List<URI> getXAddrs();

    /**
     * Gets ThisModel information.
     *
     * @return ThisModel information as defined in DPWS.
     * @see <a href="http://docs.oasis-open.org/ws-dd/dpws/1.1/os/wsdd-dpws-1.1-spec-os.html#_Toc228672093">Characteristics</a>
     */
    ThisModelType getThisModel();

    /**
     * Sets ThisModel information as defined in DPWS.
     *
     * @param thisModel ThisModel information.
     * @see <a href="http://docs.oasis-open.org/ws-dd/dpws/1.1/os/wsdd-dpws-1.1-spec-os.html#_Toc228672093">Characteristics</a>
     */
    void setThisModel(ThisModelType thisModel);

    /**
     * Gets ThisDevice information.
     *
     * @return ThisDevice information as defined in DPWS.
     * @see <a href="http://docs.oasis-open.org/ws-dd/dpws/1.1/os/wsdd-dpws-1.1-spec-os.html#_Toc228672093">Characteristics</a>
     */
    ThisDeviceType getThisDevice();

    /**
     * Set ThisDevice information as defined in DPWS.
     *
     * @param thisDevice ThisDevice information.
     * @see <a href="http://docs.oasis-open.org/ws-dd/dpws/1.1/os/wsdd-dpws-1.1-spec-os.html#_Toc228672093">Characteristics</a>
     */
    void setThisDevice(ThisDeviceType thisDevice);

    /**
     * Adds a hosted service to this hosting service.
     * <p>
     * Use {@link HostedServiceFactory} to create suitable {@link HostedService} instances.
     *
     * @param hostedService the hosted service to add.
     * @see <a href="http://docs.oasis-open.org/ws-dd/dpws/1.1/os/wsdd-dpws-1.1-spec-os.html#_Toc228672094">Hosting</a>
     */
    void addHostedService(HostedService hostedService);

    /**
     * Gets registered hosted services.
     *
     * @return a list of all registered hosted services.
     * @see <a href="http://docs.oasis-open.org/ws-dd/dpws/1.1/os/wsdd-dpws-1.1-spec-os.html#_Toc228672094">Hosting</a>
     */
    List<HostedService> getHostedServices();
}
