package org.somda.sdc.dpws.soap.wseventing;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.dpws.DpwsConstants;
import org.somda.sdc.dpws.guice.AutoRenewExecutor;
import org.somda.sdc.dpws.guice.NetworkJobThreadPool;
import org.somda.sdc.dpws.http.HttpServerRegistry;
import org.somda.sdc.dpws.soap.*;
import org.somda.sdc.dpws.soap.exception.MalformedSoapMessageException;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.dpws.soap.wsaddressing.model.ReferenceParametersType;
import org.somda.sdc.dpws.soap.wseventing.exception.SubscriptionNotFoundException;
import org.somda.sdc.dpws.soap.wseventing.factory.SubscriptionManagerFactory;
import org.somda.sdc.dpws.soap.wseventing.model.*;
import org.somda.sdc.common.util.JaxbUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import javax.annotation.Nullable;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

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
                  @Named(WsEventingConfig.AUTO_RENEW_BEFORE_EXPIRES) Duration autoRenewBeforeExpires,
                  @Named(DpwsConfig.MAX_WAIT_FOR_FUTURES) Duration maxWaitForFutures,
                  HttpServerRegistry httpServerRegistry,
                  ObjectFactory wseFactory,
                  WsAddressingUtil wsaUtil,
                  SoapMarshalling marshalling,
                  SoapUtil soapUtil,
                  JaxbUtil jaxbUtil,
                  @NetworkJobThreadPool ListeningExecutorService executorService,
                  @AutoRenewExecutor ScheduledExecutorService autoRenewExecutor,
                  SubscriptionManagerFactory subscriptionManagerFactory) {
        this.requestResponseClient = requestResponseClient;
        this.hostAddress = hostAddress;
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
        this.subscriptionManagers = new ConcurrentHashMap<>();
        this.autoRenewExecutor = autoRenewExecutor;
        this.subscriptionsLock = new ReentrantLock();
    }

    @Override
    public ListenableFuture<SubscribeResult> subscribe(List<String> actions,
                                                       @Nullable Duration expires,
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

            subscribeBody.setExpires(expires);

            subscribeBody.setFilter(filterType);

            SoapMessage subscribeRequest = soapUtil.createMessage(WsEventingConstants.WSE_ACTION_SUBSCRIBE, subscribeBody);

            // Create client to send request
            // // TODO: 19.01.2017
            //HostedServiceTransportBinding hsTb = hostedServiceTransportBindingFactory.createHostedServiceTransportBinding(hostedServiceProxy);
            //hostedServiceProxy.registerMetadataChangeObserver(hsTb);
            //RequestResponseClient hostedServiceClient = resReqClientFactory.createRequestResponseClient(hsTb);

            SoapMessage soapResponse = requestResponseClient.sendRequestResponse(subscribeRequest);
            SubscribeResponse responseBody = soapUtil.getBody(soapResponse, SubscribeResponse.class).orElseThrow(() ->
                    new MalformedSoapMessageException("Cannot read WS-Eventing Subscribe response"));

            // Create subscription manager from response
            SubscriptionManager subMan = subscriptionManagerFactory.createSourceSubscriptionManager(
                    responseBody.getSubscriptionManager(),
                    responseBody.getExpires(),
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
            renew.setExpires(expires);
            String subManAddress = wsaUtil.getAddressUriAsString(subMan.getSubscriptionManagerEpr()).orElseThrow(() ->
                    new RuntimeException("No subscription manager EPR found"));

            // Create new message, put subscription manager EPR address as wsa:To
            SoapMessage renewMsg = soapUtil.createMessage(WsEventingConstants.WSE_ACTION_RENEW, subManAddress, renew);

            // append wsa:ReferenceParameters elements to the header
            attachReferenceParameters(subMan, renewMsg);

            // Invoke request-response
            SoapMessage renewResMsg = requestResponseClient.sendRequestResponse(renewMsg);
            RenewResponse renewResponse = soapUtil.getBody(renewResMsg, RenewResponse.class).orElseThrow(() ->
                    new MalformedSoapMessageException("WS-Eventing RenewResponse message is malformed"));

            // Parse expires in response message, renew at subscription manager and return
            Duration newExpires = renewResponse.getExpires();
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
                    new RuntimeException("No subscription manager EPR found"));

            // Create new message, put subscription manager EPR address as wsa:To
            SoapMessage getStatusMsg = soapUtil.createMessage(WsEventingConstants.WSE_ACTION_GET_STATUS, subManAddress,
                    getStatus);

            // append wsa:ReferenceParameters elements to the header
            attachReferenceParameters(subMan, getStatusMsg);

            // Invoke request-response
            SoapMessage getStatusResMsg = requestResponseClient.sendRequestResponse(getStatusMsg);
            GetStatusResponse getStatusResponse = soapUtil.getBody(getStatusResMsg, GetStatusResponse.class)
                    .orElseThrow(() ->
                            new MalformedSoapMessageException("WS-Eventing GetStatusResponse message is malformed"));

            // Parse expires in response message and return
            return getStatusResponse.getExpires();
        });
    }

    @Override
    public ListenableFuture<Object> unsubscribe(String subscriptionId) {
        SinkSubscriptionManager subMan = getSubscriptionManagerProxy(subscriptionId);

        return executorService.submit(() -> {
            Unsubscribe unsubscribe = wseFactory.createUnsubscribe();
            String subManAddress = wsaUtil.getAddressUriAsString(subMan.getSubscriptionManagerEpr()).orElseThrow(() ->
                    new RuntimeException("No subscription manager EPR found"));

            // Create new message, put subscription manager EPR address as wsa:To
            SoapMessage unsubscribeMsg = soapUtil.createMessage(WsEventingConstants.WSE_ACTION_UNSUBSCRIBE, subManAddress,
                    unsubscribe);

            // append wsa:ReferenceParameters elements to the header
            attachReferenceParameters(subMan, unsubscribeMsg);

            // Invoke request-response and ignore result
            requestResponseClient.sendRequestResponse(unsubscribeMsg);
            return new Object();
        });
    }

    private void attachReferenceParameters(SinkSubscriptionManager subMan, SoapMessage unsubscribeMsg) {
        ReferenceParametersType referenceParameters = subMan.getSubscriptionManagerEpr().getReferenceParameters();
        if (referenceParameters != null) {
            List<Element> actualParameters = referenceParameters.getAny().stream()
                    // we can only reliably attach wsa:IsReferenceParameter to Element instances
                    .filter(obj -> {
                        boolean correctType = obj instanceof Element;
                        if (!correctType) {
                            LOG.warn(
                                    "reference parameter couldn't be attached to outgoing message, wrong type!" +
                                            "Type was {}", obj.getClass().getSimpleName()
                            );
                        }
                        return correctType;
                    })
                    .map(obj -> (Element) ((Element) obj).cloneNode(true))
                    .collect(Collectors.toList());
            unsubscribeMsg.getWsAddressingHeader().setMappedReferenceParameters(actualParameters);
        }
    }

    @Override
    public void unsubscribeAll() {
        for (SinkSubscriptionManager subscriptionManager : new ArrayList<>(this.subscriptionManagers.values())) {
            final ListenableFuture<Object> future = unsubscribe(subscriptionManager.getSubscriptionId());
            try {
                future.get(maxWaitForFutures.toSeconds(), TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                LOG.warn("Subscription {} could not be unsubscribed. Ignore.", subscriptionManager.getSubscriptionId());
            }
        }
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

            // Only close the output stream when the notification has been processed
            // as closing allows the server do dispatch the next request, which will cause concurrency problems
            // for the ultimate receiver of the notifications
            out.close();
        } catch (IOException e) {
            throw new TransportException(e);
        } catch (JAXBException e) {
            throw new MarshallingException(e);
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
