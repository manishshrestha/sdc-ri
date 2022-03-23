package org.somda.sdc.dpws.service;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.somda.sdc.common.util.ObjectStringifier;
import org.somda.sdc.common.util.ObjectUtil;
import org.somda.sdc.common.util.Stringified;
import org.somda.sdc.dpws.guice.JaxbDpws;
import org.somda.sdc.dpws.model.ThisDeviceType;
import org.somda.sdc.dpws.model.ThisModelType;
import org.somda.sdc.dpws.soap.RequestResponseClient;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.interception.Interceptor;
import org.somda.sdc.dpws.soap.interception.InterceptorException;

import javax.annotation.Nullable;
import javax.xml.namespace.QName;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Default implementation of {@linkplain HostingServiceProxy}.
 */
public class HostingServiceProxyImpl implements HostingServiceProxy {
    private final ObjectUtil objectUtil;

    private final RequestResponseClient requestResponseClient;
    @Stringified
    private final String activeXAddr;
    @Stringified
    private final String endpointReferenceAddress;
    private final List<QName> types;
    private final ThisDeviceType thisDevice;
    private final ThisModelType thisModel;
    private final Map<String, HostedServiceProxy> hostedServices;
    private final long metadataVersion;

    @AssistedInject
    HostingServiceProxyImpl(@Assisted("eprAddress") String endpointReferenceAddress,
                            @Assisted List<QName> types,
                            @Assisted @Nullable ThisDeviceType thisDevice,
                            @Assisted @Nullable ThisModelType thisModel,
                            @Assisted Map<String, HostedServiceProxy> hostedServices,
                            @Assisted long metadataVersion,
                            @Assisted RequestResponseClient requestResponseClient,
                            @Assisted("activeXAddr") String activeXAddr,
                            @JaxbDpws ObjectUtil objectUtil) {
        this.metadataVersion = metadataVersion;
        this.requestResponseClient = requestResponseClient;
        this.activeXAddr = activeXAddr;
        this.objectUtil = objectUtil;
        this.hostedServices = new HashMap<>(hostedServices);
        this.endpointReferenceAddress = endpointReferenceAddress;
        this.types = cloneQNames(types);
        this.thisDevice = objectUtil.deepCopyJAXB(thisDevice);
        this.thisModel = objectUtil.deepCopyJAXB(thisModel);
    }

    @Override
    public synchronized String getEndpointReferenceAddress() {
        return endpointReferenceAddress;
    }

    @Override
    public List<QName> getTypes() {
        return cloneQNames(types);
    }

    @Override
    public synchronized Optional<ThisModelType> getThisModel() {
        return Optional.ofNullable(objectUtil.deepCopyJAXB(thisModel));
    }

    @Override
    public synchronized Optional<ThisDeviceType> getThisDevice() {
        return Optional.ofNullable(objectUtil.deepCopyJAXB(thisDevice));
    }

    @Override
    public Map<String, HostedServiceProxy> getHostedServices() {
        return Collections.unmodifiableMap(hostedServices);
    }

    @Override
    public String getActiveXAddr() {
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
            throws SoapFaultException, MarshallingException, TransportException, InterceptorException {
        return requestResponseClient.sendRequestResponse(request);
    }

    @Override
    public String toString() {
        return ObjectStringifier.stringify(this);
    }

    private List<QName> cloneQNames(List<QName> qNames) {
        return qNames.stream()
                .map(qName -> new QName(qName.getLocalPart(), qName.getNamespaceURI(), qName.getPrefix()))
                .collect(Collectors.toList());
    }
}
