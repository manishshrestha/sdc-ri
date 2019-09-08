package org.ieee11073.sdc.dpws.service;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.ieee11073.sdc.common.helper.ObjectUtil;
import org.ieee11073.sdc.dpws.model.ThisDeviceType;
import org.ieee11073.sdc.dpws.model.ThisModelType;
import org.ieee11073.sdc.dpws.soap.RequestResponseClient;
import org.ieee11073.sdc.dpws.soap.SoapMessage;
import org.ieee11073.sdc.dpws.soap.exception.MarshallingException;
import org.ieee11073.sdc.dpws.soap.exception.SoapFaultException;
import org.ieee11073.sdc.dpws.soap.exception.TransportException;
import org.ieee11073.sdc.dpws.soap.interception.Interceptor;

import javax.xml.namespace.QName;
import java.net.URI;
import java.util.*;

/**
 * Default implementation of {@link HostingServiceProxy}.
 */
public class HostingServiceProxyImpl implements HostingServiceProxy {
    private final ObjectUtil objectUtil;

    private final RequestResponseClient requestResponseClient;
    private final URI activeXAddr;
    private final URI endpointReferenceAddress;
    private final List<QName> types;
    private final ThisDeviceType thisDevice;
    private final ThisModelType thisModel;
    private final Map<String, HostedServiceProxy> hostedServices;
    private final long metadataVersion;

    /**
     * @param endpointReferenceAddress
     * @param types
     * @param thisDevice
     * @param thisModel
     * @param hostedServices           Map of service ids to hosted service proxies
     * @param metadataVersion
     * @param requestResponseClient
     * @param activeXAddr
     * @param objectUtil
     */
    @AssistedInject
    HostingServiceProxyImpl(@Assisted("eprAddress") URI endpointReferenceAddress,
                            @Assisted List<QName> types,
                            @Assisted ThisDeviceType thisDevice,
                            @Assisted ThisModelType thisModel,
                            @Assisted Map<String, HostedServiceProxy> hostedServices,
                            @Assisted long metadataVersion,
                            @Assisted RequestResponseClient requestResponseClient,
                            @Assisted("activeXAddr") URI activeXAddr,
                            ObjectUtil objectUtil) {
        this.metadataVersion = metadataVersion;
        this.requestResponseClient = requestResponseClient;
        this.activeXAddr = activeXAddr;
        this.objectUtil = objectUtil;
        this.hostedServices = new HashMap<>(hostedServices);
        this.endpointReferenceAddress = objectUtil.deepCopy(endpointReferenceAddress);
        this.types = objectUtil.deepCopy(types);
        this.thisDevice = objectUtil.deepCopy(thisDevice);
        this.thisModel = objectUtil.deepCopy(thisModel);
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
    public Map<String, HostedServiceProxy> getHostedServices() {
        return Collections.unmodifiableMap(hostedServices);
    }

    @Override
    public URI getActiveXAddr() {
        return activeXAddr;
    }

    @Override
    public long getMetadataVersion() {
        return metadataVersion;
    }

    @Override
    public RequestResponseClient getRequestResponseClient() {
        return requestResponseClient;
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
