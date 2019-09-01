package org.ieee11073.sdc.dpws.soap.wseventing;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import org.ieee11073.sdc.dpws.DpwsConfig;
import org.ieee11073.sdc.dpws.DpwsConstants;
import org.ieee11073.sdc.dpws.guice.NetworkJobThreadPool;
import org.ieee11073.sdc.dpws.http.HttpServerRegistry;
import org.ieee11073.sdc.dpws.http.HttpUriBuilder;
import org.ieee11073.sdc.dpws.soap.*;
import org.ieee11073.sdc.dpws.soap.exception.MalformedSoapMessageException;
import org.ieee11073.sdc.dpws.soap.exception.MarshallingException;
import org.ieee11073.sdc.dpws.soap.exception.SoapFaultException;
import org.ieee11073.sdc.dpws.soap.exception.TransportException;
import org.ieee11073.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.ieee11073.sdc.dpws.soap.wseventing.exception.SubscriptionNotFoundException;
import org.ieee11073.sdc.dpws.soap.wseventing.factory.SubscriptionManagerFactory;
import org.ieee11073.sdc.dpws.soap.wseventing.model.*;
import org.ieee11073.sdc.common.helper.JaxbUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Default implementation of {@link EventSink}.
 */
public class EventSinkImpl implements EventSink {
    private static final Logger LOG = LoggerFactory.getLogger(EventSinkImpl.class);

    private static final String EVENT_SINK_CONTEXT_PREFIX = "/EventSink/";
    private static final String EVENT_SINK_NOTIFY_TO_CONTEXT_PREFIX = EVENT_SINK_CONTEXT_PREFIX + "NotifyTo/";
    private static final String EVENT_SINK_END_TO_CONTEXT_PREFIX = EVENT_SINK_CONTEXT_PREFIX + "EndTo/";
    private final RequestResponseClient requestResponseClient;
    private final URI hostAddress;
    private final Duration defaultRequestExpires;
    private final Duration autoRenewBeforeExpires;
    private final HttpServerRegistry httpServerRegistry;
    private final ObjectFactory wseFactory;
    private final WsAddressingUtil wsaUtil;
    private final SoapMarshalling marshalling;
    private final SoapUtil soapUtil;
    private final JaxbUtil jaxbUtil;
    private final ListeningExecutorService executorService;
    private final SubscriptionManagerFactory subscriptionManagerFactory;
    private final Map<String, SinkSubscriptionManager> subscriptionManagers;
    private final ScheduledExecutorService autoRenewExecutor;
    private final Lock subscriptionsLock;
    private final Duration maxWaitForFutures;

    @AssistedInject
    EventSinkImpl(@Assisted RequestResponseClient requestResponseClient,
                  @Assisted URI hostAddress,
                  @Named(WsEventingConfig.SINK_DEFAULT_REQUESTED_EXPIRES) Duration defaultRequestExpires,
                  @Named(WsEventingConfig.AUTO_RENEW_BEFORE_EXPIRES) Duration autoRenewBeforeExpires,
                  @Named(DpwsConfig.MAX_WAIT_FOR_FUTURES) Duration maxWaitForFutures,
                  HttpServerRegistry httpServerRegistry,
                  ObjectFactory wseFactory,
                  WsAddressingUtil wsaUtil,
                  SoapMarshalling marshalling,
                  SoapUtil soapUtil,
                  JaxbUtil jaxbUtil,
                  HttpUriBuilder uriBuilder,
                  @NetworkJobThreadPool ListeningExecutorService executorService,
                  @AutoRenewExecutor ScheduledExecutorService autoRenewExecutor,
                  SubscriptionManagerFactory subscriptionManagerFactory) {
        this.requestResponseClient = requestResponseClient;
        this.hostAddress = hostAddress;
        this.defaultRequestExpires = defaultRequestExpires;
        this.autoRenewBeforeExpires = autoRenewBeforeExpires;
        this.maxWaitForFutures = maxWaitForFutures;
        this.httpServerRegistry = httpServerRegistry;
        this.wseFactory = wseFactory;
        this.wsaUtil = wsaUtil;
        this.marshalling = marshalling;
        this.soapUtil = soapUtil;
        this.jaxbUtil = jaxbUtil;
        this.executorService = executorService;
        this.subscriptionManagerFactory = subscriptionManagerFactory;
        this.subscriptionManagers = new HashMap<>();
        this.autoRenewExecutor = autoRenewExecutor;
        this.subscriptionsLock = new ReentrantLock();
    }

    @Override
    public ListenableFuture<SubscribeResult> subscribe(List<String> actions,
                                                       Duration expires,
                                                       NotificationSink notificationSink) {
        return executorService.submit(() -> {
            //final URI httpServerBase = URI.create()
            // Create unique context path suffix
            String contextSuffix = UUID.randomUUID().toString();

            // Create unique end-to context path and create proper handler
            String endToContext = EVENT_SINK_END_TO_CONTEXT_PREFIX + contextSuffix;
            URI endToUri = httpServerRegistry.registerContext(hostAddress, endToContext,
                    (req, res, ti) -> processIncomingNotification(notificationSink, req, res));

            // Create unique notify-to context path and create proper handler
            String notifyToContext = EVENT_SINK_NOTIFY_TO_CONTEXT_PREFIX + contextSuffix;
            URI notifyToUri = httpServerRegistry.registerContext(hostAddress, notifyToContext,
                    (req, res, ti) -> processIncomingNotification(notificationSink, req, res));

            // Create subscribe body, include formerly created end-to and notify-to endpoint addresses
            // Populate rest of the request
            Subscribe subscribeBody = wseFactory.createSubscribe();

            DeliveryType deliveryType = wseFactory.createDeliveryType();
            deliveryType.setMode(WsEventingConstants.SUPPORTED_DELIVERY_MODE);

            EndpointReferenceType notifyToEpr = wsaUtil.createEprWithAddress(notifyToUri);
            deliveryType.setContent(Collections.singletonList(wseFactory.createNotifyTo(notifyToEpr)));
            subscribeBody.setDelivery(deliveryType);

            EndpointReferenceType endToEpr = wsaUtil.createEprWithAddress(endToUri);
            subscribeBody.setEndTo(endToEpr);

            FilterType filterType = wseFactory.createFilterType();
            filterType.setDialect(DpwsConstants.WS_EVENTING_SUPPORTED_DIALECT);
            filterType.setContent(Collections.singletonList(implodeUriList(actions)));

            subscribeBody.setExpires(Optional.ofNullable(expires).orElse(defaultRequestExpires).toString());

            subscribeBody.setFilter(filterType);

            SoapMessage subscribeRequest = soapUtil.createMessage(WsEventingConstants.WSA_ACTION_SUBSCRIBE, subscribeBody);

            // Create client to send request
            // // TODO: 19.01.2017
            //HostedServiceTransportBinding hsTb = hostedServiceTransportBindingFactory.createHostedServiceTransportBinding(hostedServiceProxy);
            //hostedServiceProxy.registerMetadataChangeObserver(hsTb);
            //RequestResponseClient hostedServiceClient = resReqClientFactory.createRequestResponseClient(hsTb);

            SoapMessage soapResponse = requestResponseClient.sendRequestResponse(subscribeRequest);
            SubscribeResponse responseBody = soapUtil.getBody(soapResponse, SubscribeResponse.class).orElseThrow(() ->
                    new MalformedSoapMessageException("Cannot read WS-Eventing Subscribe response."));

            // Create subscription manager from response
            SubscriptionManager subMan = subscriptionManagerFactory.createSourceSubscriptionManager(
                    responseBody.getSubscriptionManager(),
                    Duration.parse(responseBody.getExpires()),
                    notifyToEpr,
                    endToEpr,
                    null);

            // Next, create proxy object and put additional required info to it
            SinkSubscriptionManager sinkSubMan = subscriptionManagerFactory.createSinkSubscriptionManager(subMan);

            // Add sink subscription manager to internal registry
            subscriptionsLock.lock();
            try {
                subscriptionManagers.put(sinkSubMan.getSubscriptionId(), sinkSubMan);
            } finally {
                subscriptionsLock.unlock();
            }

            // Return id for addressing purposes
            return new SubscribeResult(sinkSubMan.getSubscriptionId(), sinkSubMan.getExpires());
        });
    }

    @Override
    public ListenableFuture<Duration> renew(String subscriptionId,
                                            Duration expires) {
        return executorService.submit(() -> {
            // Search for subscription to renew
            SinkSubscriptionManager subMan = getSubscriptionManagerProxy(subscriptionId);

            // Create new request body
            Renew renew = wseFactory.createRenew();
            renew.setExpires(expires.toString());
            String subManAddress = wsaUtil.getAddressUriAsString(subMan.getSubscriptionManagerEpr()).orElseThrow(() ->
                    new RuntimeException("No subscription manager EPR found."));

            // Create new message, put subscription manager EPR address as wsa:To
            SoapMessage renewMsg = soapUtil.createMessage(WsEventingConstants.WSA_ACTION_RENEW, subManAddress, renew);

            // On demand, append wse:Identifier
            jaxbUtil.extractFirstElementFromAny(subMan.getSubscriptionManagerEpr().getAny(),
                    WsEventingConstants.IDENTIFIER, String.class).ifPresent(s ->
                    renewMsg.getWsEventingHeader().setIdentifier(URI.create(s)));

            // Invoke request-response
            SoapMessage renewResMsg = requestResponseClient.sendRequestResponse(renewMsg);
            RenewResponse renewResponse = soapUtil.getBody(renewResMsg, RenewResponse.class).orElseThrow(() ->
                    new MalformedSoapMessageException("WS-Eventing RenewResponse message is malformed."));

            // Parse expires in response message, renew at subscription manager and return
            Duration newExpires = Duration.parse(renewResponse.getExpires());
            subMan.renew(newExpires);
            return newExpires;
        });
    }

    @Override
    public ListenableFuture<Duration> getStatus(String subscriptionId) {

        return executorService.submit(() -> {
            // Search for subscription to get status from
            SinkSubscriptionManager subMan = getSubscriptionManagerProxy(subscriptionId);

            GetStatus getStatus = wseFactory.createGetStatus();
            String subManAddress = wsaUtil.getAddressUriAsString(subMan.getSubscriptionManagerEpr()).orElseThrow(() ->
                    new RuntimeException("No subscription manager EPR found."));

            // Create new message, put subscription manager EPR address as wsa:To
            SoapMessage getStatusMsg = soapUtil.createMessage(WsEventingConstants.WSA_ACTION_GET_STATUS, subManAddress,
                    getStatus);

            // On demand, append wse:Identifier
            jaxbUtil.extractFirstElementFromAny(subMan.getSubscriptionManagerEpr().getAny(),
                    WsEventingConstants.IDENTIFIER, String.class).ifPresent(s ->
                    getStatusMsg.getWsEventingHeader().setIdentifier(URI.create(s)));

            // Invoke request-response
            SoapMessage getStatusResMsg = requestResponseClient.sendRequestResponse(getStatusMsg);
            GetStatusResponse getStatusResponse = soapUtil.getBody(getStatusResMsg, GetStatusResponse.class)
                    .orElseThrow(() ->
                            new MalformedSoapMessageException("WS-Eventing GetStatusResponse message is malformed."));

            // Parse expires in response message and return
            return Duration.parse(getStatusResponse.getExpires());
        });
    }

    @Override
    public ListenableFuture<Object> unsubscribe(String subscriptionId) {
        SinkSubscriptionManager subMan = getSubscriptionManagerProxy(subscriptionId);

        return executorService.submit(() -> {
            Unsubscribe unsubscribe = wseFactory.createUnsubscribe();
            String subManAddress = wsaUtil.getAddressUriAsString(subMan.getSubscriptionManagerEpr()).orElseThrow(() ->
                    new RuntimeException("No subscription manager EPR found."));

            // Create new message, put subscription manager EPR address as wsa:To
            SoapMessage unsubscribeMsg = soapUtil.createMessage(WsEventingConstants.WSA_ACTION_UNSUBSCRIBE, subManAddress,
                    unsubscribe);

            // On demand, append wse:Identifier
            jaxbUtil.extractFirstElementFromAny(subMan.getSubscriptionManagerEpr().getAny(),
                    WsEventingConstants.IDENTIFIER, String.class).ifPresent(s ->
                    unsubscribeMsg.getWsEventingHeader().setIdentifier(URI.create(s)));

            // Invoke request-response and ignore result
            requestResponseClient.sendRequestResponse(unsubscribeMsg);
            return new Object();
        });
    }

    @Override
    public void enableAutoRenew(String subscriptionId) {
        Optional.ofNullable(subscriptionManagers.get(subscriptionId)).ifPresent(subscriptionManagerProxy -> {
            subscriptionManagerProxy.setAutoRenewEnabled(true);
            autoRenewExecutor.submit(new AutoRenewTask(subscriptionManagerProxy));
        });
    }

    @Override
    public void disableAutoRenew(String subscriptionId) {
        Optional.ofNullable(subscriptionManagers.get(subscriptionId)).ifPresent(subscriptionManagerProxy ->
                subscriptionManagerProxy.setAutoRenewEnabled(false));
    }

    private String implodeUriList(List<String> actionUris) {
        StringBuilder sb = new StringBuilder();
        actionUris.forEach(s -> {
            sb.append(s);
            sb.append(" ");
        });
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    private void processIncomingNotification(NotificationSink notificationSink,
                                             InputStream in,
                                             OutputStream out) throws MarshallingException, TransportException {
        try {
            SoapMessage soapMsg = soapUtil.createMessage(marshalling.unmarshal(in));
            in.close();
            notificationSink.receiveNotification(soapMsg);
            out.close();
        } catch (IOException e) {
            throw new TransportException(e);
        } catch (JAXBException e) {
            throw new MarshallingException(e);
        }
    }

    private class AutoRenewTask implements Runnable {
        private final SinkSubscriptionManager subscriptionManager;

        AutoRenewTask(SinkSubscriptionManager subscriptionManager) {
            this.subscriptionManager = subscriptionManager;
        }

        @Override
        public void run() {
            // Quit if auto-renew is no longer enabled for subscription
            if (!subscriptionManager.isAutoRenewEnabled()) {
                return;
            }

            // Compute duration to next schedule
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expires = subscriptionManager.getExpiresTimeout();
            Duration durationLeft = Duration.between(now, expires);
            Duration nextSchedule = durationLeft.minus(autoRenewBeforeExpires);
            if (nextSchedule.isNegative() || nextSchedule.isZero()) {
                // It's time to try to renew
                Duration expiresFromRenew;
                try {
                    ListenableFuture<Duration> fut = renew(subscriptionManager.getSubscriptionId(), defaultRequestExpires);
                    expiresFromRenew = fut.get(maxWaitForFutures.toMillis(), TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    LOG.info("Auto-renew failed, because a message could not be properly delivered.", e);
                    return;
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof SoapFaultException) {
                        LOG.warn("Request of renew in {} failed. Subscription id: {}.", this.getClass().getSimpleName(),
                                subscriptionManager.getSubscriptionId());
                    } else if (e.getCause() instanceof SubscriptionNotFoundException) {
                        LOG.info("Auto-renew failed, because subscription with id '{}' does not exist anymore.",
                                subscriptionManager.getSubscriptionId());
                    } else if (e.getCause() instanceof MarshallingException) {
                        LOG.info("Auto-renew failed, because a message could not be marshalled or unmarshalled.",
                                e.getCause());
                    } else if (e.getCause() instanceof TransportException) {
                        LOG.info("Auto-renew failed, because a message could not be properly delivered.", e.getCause());
                    } else {
                        LOG.info("Unexpected exception on unsubscribe.", e.getCause());
                    }
                    return;
                } catch (TimeoutException e) {
                    LOG.info("Auto-renew failed, because the renew future did not responded in time", e);
                    return;
                }

                // Renew succeed and give new duration until subscription expires again
                // Based on that duration, compute next schedule duration
                nextSchedule = expiresFromRenew.minus(autoRenewBeforeExpires);
                if (nextSchedule.isZero() || nextSchedule.isNegative()) {
                    // It's time again to renew - submit immediate task
                    autoRenewExecutor.submit(new AutoRenewTask(subscriptionManager));
                    return;
                }
            }

            // We have some time left to invoke next renew - schedule accordingly with seconds precision
            autoRenewExecutor.schedule(new AutoRenewTask(subscriptionManager), nextSchedule.getSeconds(), TimeUnit.SECONDS);
        }
    }

    private SinkSubscriptionManager getSubscriptionManagerProxy(String subscriptionId) {
        subscriptionsLock.lock();
        try {
            return Optional.ofNullable(subscriptionManagers.get(subscriptionId))
                    .orElseThrow(SubscriptionNotFoundException::new);
        } finally {
            subscriptionsLock.unlock();
        }
    }

}
