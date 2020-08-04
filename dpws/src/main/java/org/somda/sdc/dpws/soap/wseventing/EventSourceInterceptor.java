package org.somda.sdc.dpws.soap.wseventing;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.common.util.JaxbUtil;
import org.somda.sdc.dpws.DpwsConstants;
import org.somda.sdc.dpws.device.helper.RequestResponseServerHttpHandler;
import org.somda.sdc.dpws.http.HttpServerRegistry;
import org.somda.sdc.dpws.http.HttpUriBuilder;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.factory.EnvelopeFactory;
import org.somda.sdc.dpws.soap.factory.SoapMessageFactory;
import org.somda.sdc.dpws.soap.interception.Direction;
import org.somda.sdc.dpws.soap.interception.MessageInterceptor;
import org.somda.sdc.dpws.soap.interception.RequestResponseObject;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.AttributedURIType;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.dpws.soap.wsaddressing.model.ReferenceParametersType;
import org.somda.sdc.dpws.soap.wseventing.factory.SubscriptionManagerFactory;
import org.somda.sdc.dpws.soap.wseventing.factory.WsEventingFaultFactory;
import org.somda.sdc.dpws.soap.wseventing.helper.SubscriptionRegistry;
import org.somda.sdc.dpws.soap.wseventing.model.FilterType;
import org.somda.sdc.dpws.soap.wseventing.model.GetStatus;
import org.somda.sdc.dpws.soap.wseventing.model.GetStatusResponse;
import org.somda.sdc.dpws.soap.wseventing.model.Notification;
import org.somda.sdc.dpws.soap.wseventing.model.ObjectFactory;
import org.somda.sdc.dpws.soap.wseventing.model.Renew;
import org.somda.sdc.dpws.soap.wseventing.model.RenewResponse;
import org.somda.sdc.dpws.soap.wseventing.model.Subscribe;
import org.somda.sdc.dpws.soap.wseventing.model.SubscribeResponse;
import org.somda.sdc.dpws.soap.wseventing.model.SubscriptionEnd;
import org.somda.sdc.dpws.soap.wseventing.model.Unsubscribe;
import org.somda.sdc.dpws.soap.wseventing.model.WsEventingStatus;

import javax.annotation.Nullable;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Interceptor that handles an event source's incoming subscription requests and facilitates sending notifications.
 */
public class EventSourceInterceptor extends AbstractIdleService implements EventSource {
    private static final Logger LOG = LogManager.getLogger(EventSourceInterceptor.class);

    private final Duration maxExpires;
    private final String subscriptionManagerPath;
    private final SoapUtil soapUtil;
    private final WsEventingFaultFactory faultFactory;
    private final HttpServerRegistry httpServerRegistry;
    private final Provider<RequestResponseServerHttpHandler> rrServerHttpHandlerProvider;
    private final SubscriptionRegistry subscriptionRegistry;
    private final SubscriptionManagerFactory subscriptionManagerFactory;
    private final HttpUriBuilder httpUriBuilder;
    private final Multimap<String, String> subscribedActionsToSubManIds;
    private final Lock subscribedActionsLock;


    private final JaxbUtil jaxbUtil;
    private final WsAddressingUtil wsaUtil;

    private final ObjectFactory wseFactory;
    private final SoapMessageFactory soapMessageFactory;
    private final EnvelopeFactory envelopeFactory;
    private final Logger instanceLogger;

    @Inject
    EventSourceInterceptor(@Named(WsEventingConfig.SOURCE_MAX_EXPIRES) Duration maxExpires,
                           @Named(WsEventingConfig.SOURCE_SUBSCRIPTION_MANAGER_PATH) String subscriptionManagerPath,
                           SoapUtil soapUtil,
                           WsEventingFaultFactory faultFactory,
                           JaxbUtil jaxbUtil,
                           WsAddressingUtil wsaUtil,
                           ObjectFactory wseFactory,
                           SoapMessageFactory soapMessageFactory,
                           EnvelopeFactory envelopeFactory,
                           HttpServerRegistry httpServerRegistry,
                           Provider<RequestResponseServerHttpHandler> rrServerHttpHandlerProvider,
                           SubscriptionRegistry subscriptionRegistry,
                           SubscriptionManagerFactory subscriptionManagerFactory,
                           HttpUriBuilder httpUriBuilder,
                           @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
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

        this.soapMessageFactory = soapMessageFactory;
        this.envelopeFactory = envelopeFactory;
    }

    @Override
    public void sendNotification(String action, Object payload) {
        // Remove stale subscriptions, i.e., duration expired or subscription was invalidated
        removeStaleSubscriptions();

        // Find subscription ids that are affected by the action
        subscribedActionsLock.lock();
        Set<String> affectedSubscriptionIds;
        try {
            affectedSubscriptionIds = new HashSet<>(subscribedActionsToSubManIds.get(action));
            if (affectedSubscriptionIds.isEmpty()) {
                return;
            }
        } finally {
            subscribedActionsLock.unlock();
        }

        // For each affected subscription manager create a SOAP message and add it as a Notification object to the
        // subscription manager's notification queue
        for (String subId : affectedSubscriptionIds) {
            subscriptionRegistry.getSubscription(subId).ifPresent(subscriptionManager -> {
                SoapMessage notifyTo = createForNotifyTo(action, payload, subscriptionManager);
                subscriptionManager.offerNotification(new Notification(notifyTo));
            });
        }
    }

    @Override
    public void subscriptionEndToAll(WsEventingStatus status) {
        // don't send end to stale subscription
        removeStaleSubscriptions();
        subscriptionRegistry.getSubscriptions().forEach((uri, subMan) -> {
            subMan.getEndTo().ifPresent(endTo -> {
                SoapMessage endToMessage = createForEndTo(status, subMan, endTo);
                subMan.sendToEndTo(endToMessage);
            });
            subMan.stopAsync().awaitTerminated();
        });
    }

    @MessageInterceptor(value = WsEventingConstants.WSA_ACTION_SUBSCRIBE, direction = Direction.REQUEST)
    void processSubscribe(RequestResponseObject rrObj) throws SoapFaultException {
        removeStaleSubscriptions();
        final Supplier<SoapFaultException> soapFaultExceptionSupplier = () ->
                new SoapFaultException(createInvalidMsg(rrObj));
        Subscribe subscribe = soapUtil.getBody(rrObj.getRequest(), Subscribe.class)
                .orElseThrow(soapFaultExceptionSupplier);

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

        wsaUtil.getAddressUri(notifyTo).orElseThrow(soapFaultExceptionSupplier);

        // Validate expires
        Duration grantedExpires = grantExpires(validateExpires(subscribe.getExpires()));

        // Create subscription
        var transportInfo = rrObj.getCommunicationContext().orElseThrow(() ->
                new RuntimeException("Fatal error. Missing transport information.")).getTransportInfo();
        EndpointReferenceType epr = createSubscriptionManagerEprAndRegisterHttpHandler(
                transportInfo.getScheme(),
                transportInfo.getLocalAddress().orElseThrow(() ->
                        new RuntimeException("Fatal error. Missing local address in transport information.")),
                transportInfo.getLocalPort().orElseThrow(() ->
                        new RuntimeException("Fatal error. Missing local port in transport information."))
        );

        // Validate filter type
        FilterType filterType = Optional.ofNullable(subscribe.getFilter()).orElseThrow(() ->
                new SoapFaultException(faultFactory.createEventSourceUnableToProcess("No filter given, " +
                        "but required.")));

        // Validate filter dialect
        String filterDialect = Optional.ofNullable(filterType.getDialect()).orElse("");
        if (filterDialect.isEmpty() || !filterDialect.equals(DpwsConstants.WS_EVENTING_SUPPORTED_DIALECT)) {
            throw new SoapFaultException(faultFactory.createFilteringRequestedUnavailable());
        }

        List<String> uris = explodeUriList(filterType);

        SourceSubscriptionManager subMan = subscriptionManagerFactory.createSourceSubscriptionManager(
                epr,
                grantedExpires,
                notifyTo,
                subscribe.getEndTo(),
                epr.getAddress().getValue(),
                Collections.unmodifiableList(uris)
        );

        subMan.startAsync().awaitRunning();
        // Tie together given action filter map and subscription manager
        // Store subscription manager
        subscribedActionsLock.lock();
        try {
            uris.forEach(uri -> subscribedActionsToSubManIds.put(uri, subMan.getSubscriptionId()));
        } finally {
            subscribedActionsLock.unlock();
        }

        subscriptionRegistry.addSubscription(subMan);

        // Build response body and populate response envelope
        SubscribeResponse subscribeResponse = wseFactory.createSubscribeResponse();
        subscribeResponse.setExpires(grantedExpires);

        subscribeResponse.setSubscriptionManager(subMan.getSubscriptionManagerEpr());
        soapUtil.setBody(subscribeResponse, rrObj.getResponse());
        soapUtil.setWsaAction(rrObj.getResponse(), WsEventingConstants.WSA_ACTION_SUBSCRIBE_RESPONSE);

        instanceLogger.info("Incoming subscribe request. Action(s): {}. Generated subscription id: {}. " +
                        "Notifications go to {}. Expiration in {} seconds",
                Arrays.toString(uris.toArray()),
                subMan.getSubscriptionId(),
                wsaUtil.getAddressUri(subMan.getNotifyTo()).orElse("<unknown>"),
                grantedExpires.getSeconds());
    }

    @MessageInterceptor(value = WsEventingConstants.WSA_ACTION_RENEW, direction = Direction.REQUEST)
    void processRenew(RequestResponseObject rrObj) throws SoapFaultException {
        removeStaleSubscriptions();

        Renew renew = validateRequestBody(rrObj, Renew.class);

        Duration grantedExpires = grantExpires(validateExpires(renew.getExpires()));

        SourceSubscriptionManager subMan = validateSubscriptionEpr(rrObj);
        subMan.renew(grantedExpires);


        RenewResponse renewResponse = wseFactory.createRenewResponse();
        renewResponse.setExpires(grantedExpires);
        soapUtil.setBody(renewResponse, rrObj.getResponse());
        soapUtil.setWsaAction(rrObj.getResponse(), WsEventingConstants.WSA_ACTION_RENEW_RESPONSE);

        instanceLogger.info("Subscription {} is renewed. New expiration in {} seconds",
                subMan.getSubscriptionId(),
                grantedExpires.getSeconds());
    }

    @MessageInterceptor(value = WsEventingConstants.WSA_ACTION_GET_STATUS, direction = Direction.REQUEST)
    void processGetStatus(RequestResponseObject rrObj) throws SoapFaultException {
        removeStaleSubscriptions();

        validateRequestBody(rrObj, GetStatus.class);

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
        getStatusResponse.setExpires(expires);
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

        instanceLogger.info("Unsubscribe {}. Invalidate subscription manager", subMan.getSubscriptionId());
    }

    private void removeStaleSubscriptions() {
        subscriptionRegistry.getSubscriptions().entrySet().forEach(entry -> {
            SourceSubscriptionManager subMan = entry.getValue();
            if (!subMan.isRunning() || isSubscriptionExpired(subMan)) {
                subscriptionRegistry.removeSubscription(entry.getKey());
                unregisterHttpHandler(subMan);
                subscribedActionsLock.lock();
                try {
                    HashSet<String> uris = new HashSet<>(subscribedActionsToSubManIds.keySet());
                    uris.forEach(uri ->
                            subscribedActionsToSubManIds.remove(uri, entry.getKey()));
                } finally {
                    subscribedActionsLock.unlock();
                }
                subMan.stopAsync();
                instanceLogger.info("Remove expired subscription: {}", entry.getKey());
            }
        });
    }

    private EndpointReferenceType createSubscriptionManagerEprAndRegisterHttpHandler(String scheme,
                                                                                     String address,
                                                                                     Integer port) {
        var hostPart = httpUriBuilder.buildUri(scheme, address, port);
        String contextPath = "/" + UUID.randomUUID().toString() + "/" + subscriptionManagerPath;
        String eprAddress = hostPart + contextPath;

        RequestResponseServerHttpHandler handler = rrServerHttpHandlerProvider.get();
        handler.register(this);
        httpServerRegistry.registerContext(hostPart, contextPath, handler);

        return wsaUtil.createEprWithAddress(eprAddress);
    }

    private void unregisterHttpHandler(SourceSubscriptionManager subMan) {
        var fullUri = URI.create(subMan.getSubscriptionManagerEpr().getAddress().getValue());
        var uriWithoutPath = httpUriBuilder.buildUri(fullUri.getScheme(), fullUri.getHost(), fullUri.getPort());
        httpServerRegistry.unregisterContext(uriWithoutPath, fullUri.getPath());
    }

    private boolean isSubscriptionExpired(SourceSubscriptionManager subMan) {
        Duration expires = Duration.between(LocalDateTime.now(), subMan.getExpiresTimeout());
        return expires.isZero() || expires.isNegative();
    }

    private Duration validateExpires(@Nullable Duration requestedExpires) throws SoapFaultException {
        try {
            if (requestedExpires == null) {
                return null;
            }
            if (requestedExpires.isZero() || requestedExpires.isNegative()) {
                throw new Exception(String.format("Expires is lower equal 0", requestedExpires.toString()));
            } else {
                return requestedExpires;
            }
            // CHECKSTYLE.OFF: IllegalCatch
        } catch (Exception e) {
            // CHECKSTYLE.ON: IllegalCatch
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

    private List<String> explodeUriList(FilterType filterType) {
        List<String> result = new ArrayList<>();
        if (filterType.getContent().size() != 1) {
            return result;
        }

        if (!String.class.isAssignableFrom(filterType.getContent().get(0).getClass())) {
            return result;
        }

        String listOfAnyUri = (String) filterType.getContent().get(0);
        Arrays.asList(listOfAnyUri.split("\\s+")).forEach(s -> result.add(s));

        return result;
    }

    private SoapMessage createInvalidMsg(RequestResponseObject rrObj, String reason) {
        return faultFactory.createInvalidMessage(reason, rrObj.getRequest().getOriginalEnvelope());
    }

    private SoapMessage createInvalidMsg(RequestResponseObject rrObj) {
        return createInvalidMsg(rrObj, "SOAP message is invalid.");
    }

    private Duration grantExpires(@Nullable Duration expires) {
        if (expires != null && maxExpires.compareTo(expires) >= 0) {
            return expires;
        } else {
            return maxExpires;
        }
    }

    private SoapMessage createForEndTo(WsEventingStatus status, SourceSubscriptionManager subMan,
                                       EndpointReferenceType endTo) {
        SubscriptionEnd subscriptionEnd = wseFactory.createSubscriptionEnd();
        subscriptionEnd.setSubscriptionManager(subMan.getSubscriptionManagerEpr());
        subscriptionEnd.setStatus(status.getUri());
        String wsaTo = wsaUtil.getAddressUri(endTo).orElse(null);
        return createNotification(WsEventingConstants.WSA_ACTION_SUBSCRIPTION_END, wsaTo, subscriptionEnd);
    }

    private SoapMessage createForNotifyTo(String wsaAction, Object payload, SourceSubscriptionManager subMan) {
        EndpointReferenceType notifyTo = subMan.getNotifyTo();
        String wsaTo = wsaUtil.getAddressUri(notifyTo).orElseThrow(() ->
                new RuntimeException("Could not resolve URI from NotifyTo"));
        final ReferenceParametersType referenceParameters = notifyTo.getReferenceParameters();
        return soapUtil.createMessage(wsaAction, wsaTo, payload, referenceParameters);
    }

    private SoapMessage createNotification(String wsaAction, @Nullable String wsaTo, Object payload) {
        SoapMessage msg = soapUtil.createMessage(wsaAction, payload);
        Optional.ofNullable(wsaTo).ifPresent(to ->
                msg.getWsAddressingHeader().setTo(wsaUtil.createAttributedURIType(to)));
        return msg;
    }

    public Map<String, SubscriptionManager> getActiveSubscriptions() {
        removeStaleSubscriptions();
        return subscriptionRegistry.getSubscriptions().entrySet()
                .stream().collect(Collectors.toMap(Map.Entry::getKey, e -> (SubscriptionManager) e.getValue()));
    }

    @Override
    protected void startUp() {
    }

    @Override
    protected void shutDown() {
        subscriptionEndToAll(WsEventingStatus.STATUS_SOURCE_SHUTTING_DOWN);
    }
}
