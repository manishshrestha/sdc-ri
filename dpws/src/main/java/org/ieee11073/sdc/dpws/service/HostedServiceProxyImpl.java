package org.ieee11073.sdc.dpws.service;

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.ieee11073.sdc.common.helper.ObjectUtil;
import org.ieee11073.sdc.dpws.model.HostedServiceType;
import org.ieee11073.sdc.dpws.soap.NotificationSink;
import org.ieee11073.sdc.dpws.soap.RequestResponseClient;
import org.ieee11073.sdc.dpws.soap.SoapMessage;
import org.ieee11073.sdc.dpws.soap.exception.MarshallingException;
import org.ieee11073.sdc.dpws.soap.exception.SoapFaultException;
import org.ieee11073.sdc.dpws.soap.exception.TransportException;
import org.ieee11073.sdc.dpws.soap.interception.Interceptor;
import org.ieee11073.sdc.dpws.soap.wseventing.EventSink;
import org.ieee11073.sdc.dpws.soap.wseventing.SubscribeResult;

import javax.annotation.Nullable;
import java.net.URI;
import java.time.Duration;
import java.util.List;

/**
 * Default implementation of {@link WritableHostedServiceProxy}.
 */
public class HostedServiceProxyImpl implements WritableHostedServiceProxy, EventSinkAccess {

    private final EventSink eventSink;
    private final EventBus metadataChangedBus;
    private final ObjectUtil objectUtil;
    private final Provider<NotificationSink> notificationSinkProvider;

    private HostedServiceType hostedServiceType;
    private RequestResponseClient requestResponseClient;
    private URI activeEprAddress;

    @AssistedInject
    HostedServiceProxyImpl(@Assisted HostedServiceType hostedServiceType,
                           @Assisted RequestResponseClient requestResponseClient,
                           @Assisted URI activeEprAddress,
                           @Assisted EventSink eventSink,
                           EventBus metadataChangedBus,
                           ObjectUtil objectUtil,
                           Provider<NotificationSink> notificationSinkProvider) {
        this.eventSink = eventSink;
        this.metadataChangedBus = metadataChangedBus;
        this.objectUtil = objectUtil;
        this.notificationSinkProvider = notificationSinkProvider;

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
    public EventSinkAccess getEventSinkAccess() {
        return this;
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

    @Override
    public ListenableFuture<SubscribeResult> subscribe(List<String> actions, @Nullable Duration expires, Interceptor notificationSink) {
        final NotificationSink notifications = notificationSinkProvider.get();
        notifications.register(notificationSink);
        return eventSink.subscribe(actions, expires, notifications);
    }

    @Override
    public ListenableFuture<Duration> renew(String subscriptionId, Duration expires) {
        return eventSink.renew(subscriptionId, expires);
    }

    @Override
    public ListenableFuture<Duration> getStatus(String subscriptionId) {
        return eventSink.getStatus(subscriptionId);
    }

    @Override
    public ListenableFuture unsubscribe(String subscriptionId) {
        return eventSink.unsubscribe(subscriptionId);
    }

    @Override
    public void enableAutoRenew(String subscriptionId) {
        eventSink.enableAutoRenew(subscriptionId);
    }

    @Override
    public void disableAutoRenew(String subscriptionId) {
        eventSink.disableAutoRenew(subscriptionId);
    }
}
