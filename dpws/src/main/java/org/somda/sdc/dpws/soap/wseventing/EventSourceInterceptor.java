package org.somda.sdc.dpws.soap.wseventing;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import org.somda.sdc.dpws.DpwsConstants;
import org.somda.sdc.dpws.device.helper.RequestResponseServerHttpHandler;
import org.somda.sdc.dpws.http.HttpServerRegistry;
import org.somda.sdc.dpws.http.HttpUriBuilder;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.TransportInfo;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.factory.EnvelopeFactory;
import org.somda.sdc.dpws.soap.factory.SoapMessageFactory;
import org.somda.sdc.dpws.soap.interception.Direction;
import org.somda.sdc.dpws.soap.interception.MessageInterceptor;
import org.somda.sdc.dpws.soap.interception.RequestResponseObject;
import org.somda.sdc.dpws.soap.model.Envelope;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.AttributedURIType;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.dpws.soap.wsaddressing.model.ReferenceParametersType;
import org.somda.sdc.dpws.soap.wseventing.factory.NotificationWorkerFactory;
import org.somda.sdc.dpws.soap.wseventing.factory.SubscriptionManagerFactory;
import org.somda.sdc.dpws.soap.wseventing.factory.WsEventingFaultFactory;
import org.somda.sdc.dpws.soap.wseventing.helper.EventSourceTransportManager;
import org.somda.sdc.dpws.soap.wseventing.helper.NotificationWorker;
import org.somda.sdc.dpws.soap.wseventing.helper.SubscriptionRegistry;
import org.somda.sdc.dpws.soap.wseventing.model.*;
import org.somda.sdc.common.util.JaxbUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Interceptor that handles an event source's incoming subscription requests and facilitates sending notifications.
 */
public class EventSourceInterceptor extends AbstractIdleService implements EventSource {
    private static final Logger LOG = LoggerFactory.getLogger(EventSourceInterceptor.class);

    private final Duration maxExpires;
    private final String subscriptionManagerPath;
    private final SoapUtil soapUtil;
    private final WsEventingFaultFactory faultFactory;
    private final HttpServerRegistry httpServerRegistry;
    private final Provider<RequestResponseServerHttpHandler> rrServerHttpHandlerProvider;
    private final SubscriptionRegistry subscriptionRegistry;
    private final SubscriptionManagerFactory subscriptionManagerFactory;
    private final HttpUriBuilder httpUriBuilder;
    private final Multimap<URI, String> subscribedActionsToSubManIds;
    private final Lock subscribedActionsLock;


    private final JaxbUtil jaxbUtil;
    private final WsAddressingUtil wsaUtil;

    private final ObjectFactory wseFactory;
    private final EventSourceTransportManager eventSourceTransportManager;
    private final SoapMessageFactory soapMessageFactory;
    private final EnvelopeFactory envelopeFactory;
    private final NotificationWorker notificationWorker;
    private final Thread notificationWorkerThread;

    @Inject
    EventSourceInterceptor(@Named(WsEventingConfig.SOURCE_MAX_EXPIRES) Duration maxExpires,
                           @Named(WsEventingConfig.SOURCE_SUBSCRIPTION_MANAGER_PATH) String subscriptionManagerPath,
                           SoapUtil soapUtil,
                           WsEventingFaultFactory faultFactory,
                           JaxbUtil jaxbUtil,
                           WsAddressingUtil wsaUtil,
                           ObjectFactory wseFactory,
                           EventSourceTransportManager eventSourceTransportManager,
                           SoapMessageFactory soapMessageFactory,
                           EnvelopeFactory envelopeFactory,
                           HttpServerRegistry httpServerRegistry,
                           Provider<RequestResponseServerHttpHandler> rrServerHttpHandlerProvider,
                           SubscriptionRegistry subscriptionRegistry,
                           NotificationWorkerFactory notificationWorkerFactory,
                           SubscriptionManagerFactory subscriptionManagerFactory,
                           HttpUriBuilder httpUriBuilder) {
        this.maxExpires = maxExpires;
        this.subscriptionManagerPath = subscriptionManagerPath;
        this.soapUtil = soapUtil;
        this.faultFactory = faultFactory;
        this.httpServerRegistry = httpServerRegistry;
        this.rrServerHttpHandlerProvider = rrServerHttpHandlerProvider;
        this.subscriptionRegistry = subscriptionRegistry;
        this.subscriptionManagerFactory = subscriptionManagerFactory;
        this.httpUriBuilder = httpUriBuilder;
        this.subscribedActionsToSubManIds = LinkedListMultimap.create();
        this.jaxbUtil = jaxbUtil;
        this.wsaUtil = wsaUtil;

        this.subscribedActionsLock = new ReentrantLock();
        this.wseFactory = wseFactory;

        this.eventSourceTransportManager = eventSourceTransportManager;

        this.soapMessageFactory = soapMessageFactory;
        this.envelopeFactory = envelopeFactory;
        this.notificationWorker = notificationWorkerFactory.createNotificationWorker(eventSourceTransportManager);
        this.subscriptionRegistry.registerObserver(notificationWorker);

        this.notificationWorkerThread = new Thread(notificationWorker);
    }

    @Override
    public void sendNotification(String action, Object payload) {
        // Remove stale subscriptions, i.e., duration expired or subscription was invalidated
        removeStaleSubscriptions();

        // Find subscription ids that are affected by the action
        subscribedActionsLock.lock();
        Collection<String> affectedSubscriptionIds;
        try {
            affectedSubscriptionIds = subscribedActionsToSubManIds.get(URI.create(action));
            if (affectedSubscriptionIds.isEmpty()) {
                return;
            }
        } finally {
            subscribedActionsLock.unlock();
        }

        // For each affected subscription manager create a SOAP message and add it as a Notification object to the
        // subscription manager's notification queue
        affectedSubscriptionIds.parallelStream().forEach(subId ->
                subscriptionRegistry.getSubscription(subId).ifPresent(subscriptionManager -> {
                    SoapMessage notifyTo = createForNotifyTo(action, payload, subscriptionManager);
                    subscriptionManager.getNotificationQueue().offer(new Notification(notifyTo));
                }));

        // Run worker to distribute the notification to all sinks
        notificationWorker.wakeUp();
    }

    @Override
    public void subscriptionEndToAll(WsEventingStatus status) {
        subscriptionRegistry.getSubscriptions().forEach((uri, subMan) -> {
            SoapMessage endTo = createForEndTo(status, subMan);
            eventSourceTransportManager.sendEndTo(subMan, endTo);
        });
    }

    @MessageInterceptor(value = WsEventingConstants.WSA_ACTION_SUBSCRIBE, direction = Direction.REQUEST)
    void processSubscribe(RequestResponseObject rrObj) throws SoapFaultException {
        final Supplier<SoapFaultException> soapFaultExceptionSupplier = () ->
                new SoapFaultException(createInvalidMsg(rrObj));
        Subscribe subscribe = soapUtil.getBody(rrObj.getRequest(), Subscribe.class).orElseThrow(soapFaultExceptionSupplier);

        // Validate delivery mode
        String deliveryMode = Optional.ofNullable(subscribe.getDelivery().getMode())
                .orElse(WsEventingConstants.SUPPORTED_DELIVERY_MODE);
        if (!deliveryMode.equals(WsEventingConstants.SUPPORTED_DELIVERY_MODE)) {
            throw new SoapFaultException(faultFactory.createDeliveryModeRequestedUnavailable());
        }

        // Validate delivery endpoint reference
        if (subscribe.getDelivery().getContent().size() != 1) {
            throw new SoapFaultException(createInvalidMsg(rrObj));
        }

        EndpointReferenceType notifyTo = jaxbUtil.extractElement(subscribe.getDelivery().getContent().get(0),
                WsEventingConstants.NOTIFY_TO, EndpointReferenceType.class).orElseThrow(soapFaultExceptionSupplier);

        wsaUtil.getAddressUriAsString(notifyTo).orElseThrow(soapFaultExceptionSupplier);

        // Validate expires
        Duration grantedExpires = grantExpires(validateExpires(subscribe.getExpires()));

        // Create subscription
        TransportInfo transportInfo = rrObj.getTransportInfo().orElseThrow(() ->
                new RuntimeException("Fatal error. Missing transport information."));
        EndpointReferenceType epr = createSubscriptionManagerEprAndRegisterHttpHandler(transportInfo);
        SourceSubscriptionManager subMan = subscriptionManagerFactory.createSourceSubscriptionManager(
                epr,
                grantedExpires,
                notifyTo,
                subscribe.getEndTo(),
                epr.getAddress().getValue());

        // Validate filter type
        FilterType filterType = Optional.ofNullable(subscribe.getFilter()).orElseThrow(() ->
                new SoapFaultException(faultFactory.createEventSourceUnableToProcess("No filter given, but required.")));

        // Validate filter dialect
        String filterDialect = Optional.ofNullable(filterType.getDialect()).orElse("");
        if (filterDialect.isEmpty() || !filterDialect.equals(DpwsConstants.WS_EVENTING_SUPPORTED_DIALECT)) {
            throw new SoapFaultException(faultFactory.createFilteringRequestedUnavailable());
        }

        // Tie together given action filter map and subscription manager
        // Store subscription manager
        List<URI> uris = explodeUriList(filterType);
        subscribedActionsLock.lock();
        try {
            uris.forEach(uri -> subscribedActionsToSubManIds.put(uri, subMan.getSubscriptionId()));
        } finally {
            subscribedActionsLock.unlock();
        }

        subscriptionRegistry.addSubscription(subMan);

        // Build response body and populate response envelope
        SubscribeResponse subscribeResponse = wseFactory.createSubscribeResponse();
        subscribeResponse.setExpires(grantedExpires.toString());

        subscribeResponse.setSubscriptionManager(subMan.getSubscriptionManagerEpr());
        soapUtil.setBody(subscribeResponse, rrObj.getResponse());
        soapUtil.setWsaAction(rrObj.getResponse(), WsEventingConstants.WSA_ACTION_SUBSCRIBE_RESPONSE);

        LOG.info("Incoming subscribe request. Action(s): {}. Generated subscription id: {}. Notifications go to {}. " +
                        "Expiration in {} seconds",
                Arrays.toString(uris.toArray()),
                subMan.getSubscriptionId(),
                wsaUtil.getAddressUriAsString(subMan.getNotifyTo()).orElse("<unknown>"),
                grantedExpires.getSeconds());

        subMan.startAsync().awaitRunning();
    }

    @MessageInterceptor(value = WsEventingConstants.WSA_ACTION_RENEW, direction = Direction.REQUEST)
    void processRenew(RequestResponseObject rrObj) throws SoapFaultException {
        removeStaleSubscriptions();

        Renew renew = validateRequestBody(rrObj, Renew.class);

        Duration grantedExpires = grantExpires(validateExpires(renew.getExpires()));

        SourceSubscriptionManager subMan = validateSubscriptionEpr(rrObj);
        subMan.renew(grantedExpires);


        RenewResponse renewResponse = wseFactory.createRenewResponse();
        renewResponse.setExpires(grantedExpires.toString());
        soapUtil.setBody(renewResponse, rrObj.getResponse());
        soapUtil.setWsaAction(rrObj.getResponse(), WsEventingConstants.WSA_ACTION_RENEW_RESPONSE);

        LOG.info("Subscription {} is renewed. New expiration in {} seconds",
                subMan.getSubscriptionId(),
                grantedExpires.getSeconds());
    }

    @MessageInterceptor(value = WsEventingConstants.WSA_ACTION_GET_STATUS, direction = Direction.REQUEST)
    void processGetStatus(RequestResponseObject rrObj) throws SoapFaultException {
        removeStaleSubscriptions();

        // TODO: 06.12.2016 add that somewhere...
        GetStatus getStatus = validateRequestBody(rrObj, GetStatus.class);

        Duration expires;
        subscribedActionsLock.lock();
        try {
            SourceSubscriptionManager subMan = validateSubscriptionEpr(rrObj);
            expires = Duration.between(LocalDateTime.now(), subMan.getExpiresTimeout());
            if (expires.isNegative() || expires.isZero()) {
                throw new SoapFaultException(createInvalidMsg(rrObj,
                        String.format("Given wse:Identifier '%s' is invalid.", subMan.getSubscriptionId())));
            }
        } finally {
            subscribedActionsLock.unlock();
        }

        GetStatusResponse getStatusResponse = wseFactory.createGetStatusResponse();
        getStatusResponse.setExpires(expires.toString());
        soapUtil.setBody(getStatusResponse, rrObj.getResponse());
        soapUtil.setWsaAction(rrObj.getResponse(), WsEventingConstants.WSA_ACTION_GET_STATUS_RESPONSE);
    }

    @MessageInterceptor(value = WsEventingConstants.WSA_ACTION_UNSUBSCRIBE, direction = Direction.REQUEST)
    void processUnsubscribe(RequestResponseObject rrObj) throws SoapFaultException {
        removeStaleSubscriptions();

        validateRequestBody(rrObj, Unsubscribe.class);

        SourceSubscriptionManager subMan = validateSubscriptionEpr(rrObj);
        subMan.stopAsync().awaitTerminated();

        // No response body required
        soapUtil.setWsaAction(rrObj.getResponse(), WsEventingConstants.WSA_ACTION_UNSUBSCRIBE_RESPONSE);

        LOG.info("Unsubscribe {}. Invalidate subscription manager", subMan.getSubscriptionId());
    }

    private void removeStaleSubscriptions() {
        subscriptionRegistry.getSubscriptions().entrySet().parallelStream().forEach(entry -> {
            SourceSubscriptionManager subMan = entry.getValue();
            if (!subMan.isRunning() || isSubscriptionExpired(subMan)) {
                subscriptionRegistry.removeSubscription(entry.getKey());
                unregisterHttpHandler(subMan);
                subscribedActionsLock.lock();
                try {
                    HashSet<URI> uris = new HashSet<>(subscribedActionsToSubManIds.keySet());
                    uris.forEach(uri ->
                            subscribedActionsToSubManIds.remove(uri, entry.getKey()));
                } finally {
                    subscribedActionsLock.unlock();
                }
                LOG.info("Remove expired subscription: {}", entry.getKey());
            }
        });
    }

    private EndpointReferenceType createSubscriptionManagerEprAndRegisterHttpHandler(TransportInfo transportInfo) {
        final URI hostPart = httpUriBuilder.buildUri(
                transportInfo.getScheme(), transportInfo.getLocalAddress(), transportInfo.getLocalPort());
        String contextPath = "/" + subscriptionManagerPath + "/" + UUID.randomUUID().toString();
        String eprAddress = hostPart + contextPath;

        RequestResponseServerHttpHandler handler = rrServerHttpHandlerProvider.get();
        handler.register(this);
        httpServerRegistry.registerContext(hostPart, contextPath, handler);

        return wsaUtil.createEprWithAddress(eprAddress);
    }

    private void unregisterHttpHandler(SourceSubscriptionManager subMan) {
        URI fullUri = URI.create(subMan.getSubscriptionManagerEpr().getAddress().getValue());
        URI uriWithoutPath = httpUriBuilder.buildUri(fullUri.getScheme(), fullUri.getHost(), fullUri.getPort());
        httpServerRegistry.unregisterContext(uriWithoutPath, fullUri.getPath());
    }

    private boolean isSubscriptionExpired(SourceSubscriptionManager subMan) {
        Duration expires = Duration.between(LocalDateTime.now(), subMan.getExpiresTimeout());
        return expires.isZero() || expires.isNegative();
    }

    private Duration validateExpires(String expires) throws SoapFaultException {
        try {
            Duration requestedExpires = Duration.parse(expires);
            if (requestedExpires.isZero() || requestedExpires.isNegative()) {
                throw new Exception();
            } else {
                return requestedExpires;
            }
        } catch (Exception e) {
            throw new SoapFaultException(faultFactory.createInvalidExpirationTime());
        }
    }

    private <T> T validateRequestBody(RequestResponseObject rrObj, Class<T> expectedType) throws SoapFaultException {
        return soapUtil.getBody(rrObj.getRequest(), expectedType).orElseThrow(() ->
                new SoapFaultException(createInvalidMsg(rrObj)));
    }

    private SourceSubscriptionManager validateSubscriptionEpr(RequestResponseObject rrObj) throws SoapFaultException {
        AttributedURIType toUri = rrObj.getRequest().getWsAddressingHeader().getTo().orElseThrow(() ->
                new SoapFaultException(createInvalidMsg(rrObj)));

        return subscriptionRegistry.getSubscription(toUri.getValue()).orElseThrow(() ->
                new SoapFaultException(createInvalidMsg(rrObj,
                        String.format("Subscription manager '%s' does not exist.", toUri.getValue()))));
    }

    private List<URI> explodeUriList(FilterType filterType) {
        List<URI> result = new ArrayList<>();
        if (filterType.getContent().size() != 1) {
            return result;
        }

        if (!String.class.isAssignableFrom(filterType.getContent().get(0).getClass())) {
            return result;
        }

        String listOfAnyUri = (String) filterType.getContent().get(0);
        Arrays.asList(listOfAnyUri.split("\\s+")).forEach(s -> result.add(URI.create(s)));

        return result;
    }

    private SoapMessage createInvalidMsg(RequestResponseObject rrObj, String reason) {
        return faultFactory.createInvalidMessage(reason, rrObj.getRequest().getOriginalEnvelope());
    }

    private SoapMessage createInvalidMsg(RequestResponseObject rrObj) {
        return createInvalidMsg(rrObj, "SOAP message is invalid.");
    }

    private Duration grantExpires(Duration expires) {
        if (maxExpires.compareTo(expires) >= 0) {
            return expires;
        } else {
            return maxExpires;
        }
    }

    private SoapMessage createForEndTo(WsEventingStatus status, SourceSubscriptionManager subMan) {
        SubscriptionEnd subscriptionEnd = wseFactory.createSubscriptionEnd();
        subscriptionEnd.setSubscriptionManager(subMan.getSubscriptionManagerEpr());
        subscriptionEnd.setStatus(status.getUri());
        String wsaTo = wsaUtil.getAddressUriAsString(subMan.getEndTo()).orElse(null);
        return createNotification(WsEventingConstants.WSA_ACTION_SUBSCRIPTION_END, wsaTo, subscriptionEnd);
    }

    private SoapMessage createForNotifyTo(String wsaAction, Object payload, SourceSubscriptionManager subMan) {
        EndpointReferenceType notifyTo = subMan.getNotifyTo();
        String wsaTo = wsaUtil.getAddressUriAsString(notifyTo).orElseThrow(() ->
                new RuntimeException("Could not resolve URI from NotifyTo"));
        Envelope envelope = envelopeFactory.createEnvelope(wsaAction, wsaTo, payload);
        final ReferenceParametersType referenceParameters = notifyTo.getReferenceParameters();
        if (referenceParameters != null) {
            referenceParameters.getAny().forEach(refParam -> envelope.getHeader().getAny().add(refParam));
        }
        return soapMessageFactory.createSoapMessage(envelope);
    }

    private SoapMessage createNotification(String wsaAction, @Nullable String wsaTo, Object payload) {
        SoapMessage msg = soapMessageFactory.createSoapMessage(envelopeFactory.createEnvelope(wsaAction, payload));
        Optional.ofNullable(wsaTo).ifPresent(to ->
                msg.getWsAddressingHeader().setTo(wsaUtil.createAttributedURIType(to)));
        return msg;
    }

    @Override
    protected void startUp() {
        subscriptionRegistry.registerObserver(eventSourceTransportManager);
        notificationWorkerThread.start();
    }

    @Override
    protected void shutDown() {
        notificationWorkerThread.interrupt();
        subscriptionEndToAll(WsEventingStatus.STATUS_SOURCE_SHUTTING_DOWN);
        subscriptionRegistry.unregisterObserver(eventSourceTransportManager);
    }
}
