package org.ieee11073.sdc.dpws.service;

import com.google.common.eventbus.EventBus;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.ieee11073.sdc.dpws.helper.PeerInformation;
import org.ieee11073.sdc.dpws.model.HostedServiceType;
import org.ieee11073.sdc.dpws.soap.RequestResponseClient;
import org.ieee11073.sdc.dpws.soap.SoapMessage;
import org.ieee11073.sdc.dpws.soap.exception.MarshallingException;
import org.ieee11073.sdc.dpws.soap.exception.SoapFaultException;
import org.ieee11073.sdc.dpws.soap.exception.TransportException;
import org.ieee11073.sdc.dpws.soap.interception.Interceptor;
import org.ieee11073.sdc.common.helper.ObjectUtil;

import javax.annotation.Nullable;
import java.net.URI;

/**
 * Default implementation of {@link WritableHostedServiceProxy}.
 */
public class HostedServiceProxyImpl implements WritableHostedServiceProxy {

    private final EventBus metadataChangedBus;
    private final ObjectUtil objectUtil;

    private HostedServiceType hostedServiceType;
    private RequestResponseClient requestResponseClient;
    private URI activeEprAddress;

    @AssistedInject
    HostedServiceProxyImpl(@Assisted HostedServiceType hostedServiceType,
                           @Assisted RequestResponseClient requestResponseClient,
                           @Assisted URI activeEprAddress,
                           EventBus metadataChangedBus,
                           ObjectUtil objectUtil) {
        this.metadataChangedBus = metadataChangedBus;
        this.objectUtil = objectUtil;

        updateProxyInformation(hostedServiceType, requestResponseClient, activeEprAddress);
    }

    @Override
    public synchronized void updateProxyInformation(HostedServiceType type,
                                                    RequestResponseClient requestResponseClient,
                                                    URI activeEprAddress) {
        HostedServiceType typeBefore = getType();
        this.hostedServiceType = objectUtil.deepCopy(type);
        this.requestResponseClient = requestResponseClient;
        this.activeEprAddress = activeEprAddress;

        metadataChangedBus.post(new HostedServiceMetadataChangeMessage(
                typeBefore,
                getType(),
                getRequestResponseClient(),
                getActiveEprAddress()));
    }

    @Override
    public synchronized HostedServiceType getType() {
        return objectUtil.deepCopy(hostedServiceType);
    }

    @Override
    public synchronized RequestResponseClient getRequestResponseClient() {
        return requestResponseClient;
    }

    @Override
    public void registerMetadataChangeObserver(HostedServiceMetadataObserver observer) {
        metadataChangedBus.register(observer);
    }

    @Override
    public void unregisterMetadataChangeObserver(HostedServiceMetadataObserver observer) {
        metadataChangedBus.unregister(observer);
    }

    @Override
    public synchronized URI getActiveEprAddress() {
        return activeEprAddress;
    }

    @Override
    public synchronized void register(Interceptor interceptor) {
        requestResponseClient.register(interceptor);
    }

    @Override
    public synchronized SoapMessage sendRequestResponse(SoapMessage request) throws SoapFaultException, MarshallingException, TransportException {
        return requestResponseClient.sendRequestResponse(request);
    }
}
