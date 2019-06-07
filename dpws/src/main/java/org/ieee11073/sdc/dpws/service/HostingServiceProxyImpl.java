package org.ieee11073.sdc.dpws.service;

import com.google.common.eventbus.EventBus;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
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
    private URI activeXAddr;
    private URI endpointReferenceAddress;
    private List<QName> types;
    private ThisDeviceType thisDevice;
    private ThisModelType thisModel;
    private Map<String, WritableHostedServiceProxy> hostedServices;
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
     * @param activeXAddr
     * @param objectUtil
     * @param eventBus
     */
    @AssistedInject
    HostingServiceProxyImpl(@Assisted("eprAddress") URI endpointReferenceAddress,
                            @Assisted List<QName> types,
                            @Assisted ThisDeviceType thisDevice,
                            @Assisted ThisModelType thisModel,
                            @Assisted Map<String, WritableHostedServiceProxy> hostedServices,
                            @Assisted long metadataVersion,
                            @Assisted RequestResponseClient requestResponseClient,
                            @Assisted("activeXAddr") URI activeXAddr,
                            ObjectUtil objectUtil,
                            EventBus eventBus) {
        this.metadataVersion = metadataVersion;
        this.requestResponseClient = requestResponseClient;
        this.activeXAddr = activeXAddr;
        this.objectUtil = objectUtil;
        this.eventBus = eventBus;
        this.hostedServices = new HashMap<String, WritableHostedServiceProxy>();

        updateProxyInformation(
                endpointReferenceAddress,
                types,
                thisDevice,
                thisModel,
                hostedServices,
                metadataVersion,
                requestResponseClient,
                activeXAddr);
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
    public synchronized Map<String, HostedServiceProxy> getHostedServices() {
        return new HashMap<>(hostedServices);
    }

    @Override
    public synchronized Map<String, WritableHostedServiceProxy> getWritableHostedServices() {
        return new HashMap<String, WritableHostedServiceProxy>(hostedServices);
    }

    @Override
    public synchronized URI getActiveXAddr() {
        return activeXAddr;
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
                hsProxy.getActiveXAddr());
    }

    @Override
    public synchronized void updateProxyInformation(URI endpointReferenceAddress,
                                                    List<QName> types,
                                                    @Nullable ThisDeviceType thisDevice,
                                                    @Nullable ThisModelType thisModel,
                                                    Map<String, WritableHostedServiceProxy> hostedServices,
                                                    long metadataVersion,
                                                    RequestResponseClient requestResponseClient,
                                                    URI activeXAddr) {
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
        this.activeXAddr = activeXAddr;

        Map<String, HostedServiceProxy> hostedServicesBefore = getHostedServices();
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
                activeXAddr
        ));
    }

    private synchronized void updateHostedServices(Map<String, WritableHostedServiceProxy> updatedHostedServices) {
        Map<String, WritableHostedServiceProxy> updatedHostedServicesCpy = new HashMap<>(updatedHostedServices);
        Map<String, WritableHostedServiceProxy> currentHostedServicesCpy = getWritableHostedServices();

        for (Map.Entry<String, WritableHostedServiceProxy> hostedService : currentHostedServicesCpy.entrySet()) {
            String serviceId = hostedService.getKey();
            WritableHostedServiceProxy updatedHostedService = updatedHostedServicesCpy.get(serviceId);
            if (updatedHostedService == null) {
                hostedServices.remove(serviceId);
            } else {
                hostedService.getValue().updateProxyInformation(updatedHostedService.getType(), requestResponseClient,
                        activeXAddr);
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
