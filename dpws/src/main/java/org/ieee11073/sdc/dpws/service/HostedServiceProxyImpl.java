package org.ieee11073.sdc.dpws.service;

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
import org.ieee11073.sdc.dpws.soap.interception.InterceptorException;
import org.ieee11073.sdc.dpws.soap.wseventing.EventSink;
import org.ieee11073.sdc.dpws.soap.wseventing.SubscribeResult;

import javax.annotation.Nullable;
import java.net.URI;
import java.time.Duration;
import java.util.List;

/**
 * Default implementation of {@linkplain HostedServiceProxy}.
 */
public class HostedServiceProxyImpl implements HostedServiceProxy, EventSinkAccess {

    private final EventSink eventSink;
    private final ObjectUtil objectUtil;
    private final Provider<NotificationSink> notificationSinkProvider;

    private final HostedServiceType hostedServiceType;
    private final RequestResponseClient requestResponseClient;
    private final URI activeEprAddress;

    @AssistedInject
    HostedServiceProxyImpl(@Assisted HostedServiceType hostedServiceType,
                           @Assisted RequestResponseClient requestResponseClient,
                           @Assisted URI activeEprAddress,
                           @Assisted EventSink eventSink,
                           ObjectUtil objectUtil,
                           Provider<NotificationSink> notificationSinkProvider) {
        this.eventSink = eventSink;
        this.objectUtil = objectUtil;
        this.notificationSinkProvider = notificationSinkProvider;
        this.hostedServiceType = objectUtil.deepCopy(hostedServiceType);
        this.requestResponseClient = requestResponseClient;
        this.activeEprAddress = activeEprAddress;
    }

    @Override
    public HostedServiceType getType() {
        return objectUtil.deepCopy(hostedServiceType);
    }

    @Override
    public RequestResponseClient getRequestResponseClient() {
        return requestResponseClient;
    }

    @Override
    public EventSinkAccess getEventSinkAccess() {
        return this;
    }

    @Override
    public URI getActiveEprAddress() {
        return activeEprAddress;
    }

    @Override
    public synchronized void register(Interceptor interceptor) {
        requestResponseClient.register(interceptor);
    }

    @Override
    public synchronized SoapMessage sendRequestResponse(SoapMessage request) throws SoapFaultException, MarshallingException, TransportException, InterceptorException {
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
