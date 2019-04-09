package org.ieee11073.sdc.dpws.service;

import com.google.common.eventbus.EventBus;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.ieee11073.sdc.dpws.helper.PeerInformation;
import org.ieee11073.sdc.dpws.model.ThisDeviceType;
import org.ieee11073.sdc.dpws.model.ThisModelType;
import org.ieee11073.sdc.dpws.soap.RequestResponseClient;
import org.ieee11073.sdc.dpws.soap.SoapMessage;
import org.ieee11073.sdc.dpws.soap.exception.MarshallingException;
import org.ieee11073.sdc.dpws.soap.exception.SoapFaultException;
import org.ieee11073.sdc.dpws.soap.exception.TransportException;
import org.ieee11073.sdc.dpws.soap.interception.Interceptor;
import org.ieee11073.sdc.common.helper.ObjectUtil;

import javax.annotation.Nullable;
import javax.xml.namespace.QName;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Default implementation of {@link HostingServiceProxy}.
 */
public class HostingServiceProxyImpl implements WritableHostingServiceProxy {
    private final ObjectUtil objectUtil;
    private final EventBus eventBus;

    private RequestResponseClient requestResponseClient;
    private PeerInformation peerInformation;
    private URI endpointReferenceAddress;
    private List<QName> types;
    private ThisDeviceType thisDevice;
    private ThisModelType thisModel;
    private Map<URI, WritableHostedServiceProxy> hostedServices;
    private long metadataVersion;

    /**
     *
     * @param endpointReferenceAddress
     * @param types
     * @param thisDevice
     * @param thisModel
     * @param hostedServices Map of service ids to hosted service proxies
     * @param metadataVersion
     * @param requestResponseClient
     * @param peerInformation
     * @param objectUtil
     * @param eventBus
     */
    @AssistedInject
    HostingServiceProxyImpl(@Assisted URI endpointReferenceAddress,
                            @Assisted List<QName> types,
                            @Assisted ThisDeviceType thisDevice,
                            @Assisted ThisModelType thisModel,
                            @Assisted Map<URI, WritableHostedServiceProxy> hostedServices,
                            @Assisted long metadataVersion,
                            @Assisted RequestResponseClient requestResponseClient,
                            @Assisted PeerInformation peerInformation,
                            ObjectUtil objectUtil,
                            EventBus eventBus) {
        this.metadataVersion = metadataVersion;
        this.requestResponseClient = requestResponseClient;
        this.peerInformation = peerInformation;
        this.objectUtil = objectUtil;
        this.eventBus = eventBus;
        this.hostedServices = new HashMap<>();

        updateProxyInformation(
                endpointReferenceAddress,
                types,
                thisDevice,
                thisModel,
                hostedServices,
                metadataVersion,
                requestResponseClient,
                peerInformation);
    }

    @Override
    public synchronized URI getEndpointReferenceAddress() {
        return endpointReferenceAddress;
    }

    @Override
    public List<QName> getTypes() {
        return objectUtil.deepCopy(types);
    }

    @Override
    public synchronized Optional<ThisModelType> getThisModel() {
        return Optional.ofNullable(objectUtil.deepCopy(thisModel));
    }

    @Override
    public synchronized Optional<ThisDeviceType> getThisDevice() {
        return Optional.ofNullable(objectUtil.deepCopy(thisDevice));
    }

    @Override
    public synchronized Map<URI, HostedServiceProxy> getHostedServices() {
        return new HashMap<>(hostedServices);
    }

    @Override
    public synchronized Map<URI, WritableHostedServiceProxy> getWritableHostedServices() {
        return new HashMap<>(hostedServices);
    }

    @Override
    public synchronized PeerInformation getPeerInformation() {
        return peerInformation;
    }

    @Override
    public synchronized long getMetadataVersion() {
        return metadataVersion;
    }

    @Override
    public synchronized RequestResponseClient getRequestResponseClient() {
        return requestResponseClient;
    }

    @Override
    public synchronized void updateProxyInformation(WritableHostingServiceProxy hsProxy) {
        updateProxyInformation(
                hsProxy.getEndpointReferenceAddress(),
                hsProxy.getTypes(),
                hsProxy.getThisDevice().orElse(null),
                hsProxy.getThisModel().orElse(null),
                hsProxy.getWritableHostedServices(),
                hsProxy.getMetadataVersion(),
                hsProxy.getRequestResponseClient(),
                hsProxy.getPeerInformation());
    }

    @Override
    public synchronized void updateProxyInformation(URI endpointReferenceAddress,
                                                    List<QName> types,
                                                    @Nullable ThisDeviceType thisDevice,
                                                    @Nullable ThisModelType thisModel,
                                                    Map<URI, WritableHostedServiceProxy> hostedServices,
                                                    long metadataVersion,
                                                    RequestResponseClient requestResponseClient,
                                                    PeerInformation peerInformation) {
        URI endpointReferenceAddressBefore = getEndpointReferenceAddress();
        this.endpointReferenceAddress = objectUtil.deepCopy(endpointReferenceAddress);
        List<QName> typesBefore = getTypes();
        this.types = objectUtil.deepCopy(types);
        ThisDeviceType thisDeviceBefore = getThisDevice().orElse(null);
        this.thisDevice = objectUtil.deepCopy(thisDevice);
        ThisModelType thisModelBefore = getThisModel().orElse(null);
        this.thisModel = objectUtil.deepCopy(thisModel);
        long metadataVersionBefore = getMetadataVersion();
        this.metadataVersion = metadataVersion;
        this.requestResponseClient = requestResponseClient;
        this.peerInformation = peerInformation;

        Map<URI, HostedServiceProxy> hostedServicesBefore = getHostedServices();
        updateHostedServices(hostedServices);

        eventBus.post(new HostingServiceMetadataChangeMessage(
                endpointReferenceAddressBefore,
                getEndpointReferenceAddress(),
                typesBefore,
                getTypes(),
                Optional.ofNullable(thisDeviceBefore),
                getThisDevice(),
                Optional.ofNullable(thisModelBefore),
                getThisModel(),
                metadataVersionBefore,
                getMetadataVersion(),
                hostedServicesBefore,
                getHostedServices(),
                requestResponseClient,
                peerInformation
        ));
    }

    private synchronized void updateHostedServices(Map<URI, WritableHostedServiceProxy> updatedHostedServices) {
        Map<URI, WritableHostedServiceProxy> updatedHostedServicesCpy = new HashMap<>(updatedHostedServices);
        Map<URI, WritableHostedServiceProxy> currentHostedServicesCpy = getWritableHostedServices();

        for (Map.Entry<URI, WritableHostedServiceProxy> hostedService : currentHostedServicesCpy.entrySet()) {
            URI serviceId = hostedService.getKey();
            WritableHostedServiceProxy updatedHostedService = updatedHostedServicesCpy.get(serviceId);
            if (updatedHostedService == null) {
                hostedServices.remove(serviceId);
            } else {
                hostedService.getValue().updateProxyInformation(updatedHostedService.getType(), requestResponseClient,
                        peerInformation);
                updatedHostedServicesCpy.remove(serviceId);
            }
        }

        hostedServices.putAll(updatedHostedServicesCpy);
    }

    @Override
    public synchronized void register(Interceptor interceptor) {
        requestResponseClient.register(interceptor);
    }

    @Override
    public synchronized SoapMessage sendRequestResponse(SoapMessage request)
            throws SoapFaultException, MarshallingException, TransportException {
        return requestResponseClient.sendRequestResponse(request);
    }
}
