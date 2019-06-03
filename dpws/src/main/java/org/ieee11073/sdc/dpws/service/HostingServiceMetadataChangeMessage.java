package org.ieee11073.sdc.dpws.service;

import org.ieee11073.sdc.common.event.EventMessage;
import org.ieee11073.sdc.dpws.model.ThisDeviceType;
import org.ieee11073.sdc.dpws.model.ThisModelType;
import org.ieee11073.sdc.dpws.soap.RequestResponseClient;

import javax.xml.namespace.QName;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Changeset on changes of hosting service metadata.
 */
public class HostingServiceMetadataChangeMessage implements EventMessage {
    private final URI endpointReferenceAddressBefore;
    private final URI endpointReferenceAddressAfter;
    private final List<QName> typesBefore;
    private final List<QName> typesAfter;
    private final Optional<ThisDeviceType> thisDeviceBefore;
    private final Optional<ThisDeviceType> thisDeviceAfter;
    private final Optional<ThisModelType> thisModelBefore;
    private final Optional<ThisModelType> thisModelAfter;
    private final long metadataVersionBefore;
    private final long metadataVersionAfter;
    private final Map<URI, HostedServiceProxy> hostedServicesBefore;
    private final Map<URI, HostedServiceProxy> hostedServicesAfter;
    private final RequestResponseClient requestResponseClient;
    private final URI activeXAddr;

    public HostingServiceMetadataChangeMessage(URI endpointReferenceAddressBefore,
                                               URI endpointReferenceAddressAfter,
                                               List<QName> typesBefore,
                                               List<QName> typesAfter,
                                               Optional<ThisDeviceType> thisDeviceBefore,
                                               Optional<ThisDeviceType> thisDeviceAfter,
                                               Optional<ThisModelType> thisModelBefore,
                                               Optional<ThisModelType> thisModelAfter,
                                               long metadataVersionBefore,
                                               long metadataVersionAfter,
                                               Map<URI, HostedServiceProxy> hostedServicesBefore,
                                               Map<URI, HostedServiceProxy> hostedServicesAfter,
                                               RequestResponseClient requestResponseClient,
                                               URI activeXAddr) {
        this.endpointReferenceAddressBefore = endpointReferenceAddressBefore;
        this.endpointReferenceAddressAfter = endpointReferenceAddressAfter;
        this.typesBefore = typesBefore;
        this.typesAfter = typesAfter;
        this.thisDeviceBefore = thisDeviceBefore;
        this.thisDeviceAfter  = thisDeviceAfter;
        this.thisModelBefore = thisModelBefore;
        this.thisModelAfter = thisModelAfter;
        this.metadataVersionBefore = metadataVersionBefore;
        this.metadataVersionAfter = metadataVersionAfter;
        this.hostedServicesBefore = hostedServicesBefore;
        this.hostedServicesAfter = hostedServicesAfter;
        this.requestResponseClient = requestResponseClient;
        this.activeXAddr = activeXAddr;
    }

    public URI getEndpointReferenceAddressBefore() {
        return endpointReferenceAddressBefore;
    }


    public URI getEndpointReferenceAddressAfter() {
        return endpointReferenceAddressAfter;
    }

    public List<QName> getTypesBefore() {
        return typesBefore;
    }

    public List<QName> getTypesAfter() {
        return typesAfter;
    }

    public Optional<ThisDeviceType> getThisDeviceBefore() {
        return thisDeviceBefore;
    }

    public Optional<ThisDeviceType> getThisDeviceAfter() {
        return thisDeviceAfter;
    }

    public Optional<ThisModelType> getThisModelBefore() {
        return thisModelBefore;
    }

    public Optional<ThisModelType> getThisModelAfter() {
        return thisModelAfter;
    }

    public long getMetadataVersionBefore() {
        return metadataVersionBefore;
    }

    public long getMetadataVersionAfter() {
        return metadataVersionAfter;
    }

    public Map<URI, HostedServiceProxy> getHostedServicesBefore() {
        return hostedServicesBefore;
    }

    public Map<URI, HostedServiceProxy> getHostedServicesAfter() {
        return hostedServicesAfter;
    }

    public RequestResponseClient getRequestResponseClientAfter() {
        return requestResponseClient;
    }

    public URI getActiveXAddr() {
        return activeXAddr;
    }
}
