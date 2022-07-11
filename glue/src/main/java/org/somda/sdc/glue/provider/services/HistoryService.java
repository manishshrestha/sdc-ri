package org.somda.sdc.glue.provider.services;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import org.somda.sdc.biceps.model.history.ChangeSequenceReportType;
import org.somda.sdc.biceps.model.history.HistoryQueryType;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.common.util.ExecutorWrapperService;
import org.somda.sdc.common.util.JaxbUtil;
import org.somda.sdc.dpws.DpwsConstants;
import org.somda.sdc.dpws.device.WebService;
import org.somda.sdc.dpws.guice.NetworkJobThreadPool;
import org.somda.sdc.dpws.http.HttpUriBuilder;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.TransportInfo;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.factory.SoapFaultFactory;
import org.somda.sdc.dpws.soap.interception.RequestResponseObject;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.dpws.soap.wseventing.EventSourceFilterPlugin;
import org.somda.sdc.dpws.soap.wseventing.SourceSubscriptionManager;
import org.somda.sdc.dpws.soap.wseventing.WsEventingConfig;
import org.somda.sdc.dpws.soap.wseventing.WsEventingConstants;
import org.somda.sdc.dpws.soap.wseventing.factory.SubscriptionManagerFactory;
import org.somda.sdc.dpws.soap.wseventing.model.Notification;
import org.somda.sdc.dpws.soap.wseventing.model.ObjectFactory;
import org.somda.sdc.dpws.soap.wseventing.model.Subscribe;
import org.somda.sdc.dpws.soap.wseventing.model.SubscribeResponse;
import org.somda.sdc.dpws.soap.wseventing.model.SubscriptionEnd;
import org.somda.sdc.dpws.soap.wseventing.model.WsEventingStatus;
import org.somda.sdc.glue.provider.services.helper.MdibRevisionObserver;
import org.somda.sdc.glue.provider.services.helper.factory.MdibRevisionObserverFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.somda.sdc.glue.common.ActionConstants.ACTION_HISTORY_MDIB_REPORT;

/**
 * Implementation of the History service which is used to query historical MDIB data.
 */
public class HistoryService extends WebService implements EventSourceFilterPlugin {
    private final String subscriptionManagerPath;
    private final Duration maxExpires;
    private final HttpUriBuilder httpUriBuilder;
    private final SoapUtil soapUtil;
    private final ObjectFactory wseFactory;
    private final org.somda.sdc.biceps.model.history.ObjectFactory historyObjectFactory;
    private final WsAddressingUtil wsaUtil;
    private final SoapFaultFactory soapFaultFactory;
    private final SubscriptionManagerFactory subscriptionManagerFactory;
    private final MdibRevisionObserver mdibRevisionObserver;
    private final JaxbUtil jaxbUtil;
    private final ExecutorWrapperService<ListeningExecutorService> networkJobExecutor;

    @Inject
    HistoryService(@Assisted LocalMdibAccess mdibAccess,
                   @Named(WsEventingConfig.SOURCE_SUBSCRIPTION_MANAGER_PATH) String subscriptionManagerPath,
                   @Named(WsEventingConfig.SOURCE_MAX_EXPIRES) Duration maxExpires,
                   HttpUriBuilder httpUriBuilder,
                   SoapUtil soapUtil,
                   ObjectFactory wseFactory,
                   org.somda.sdc.biceps.model.history.ObjectFactory historyObjectFactory,
                   WsAddressingUtil wsaUtil,
                   SoapFaultFactory soapFaultFactory,
                   SubscriptionManagerFactory subscriptionManagerFactory,
                   MdibRevisionObserverFactory mdibRevisionObserverFactory,
                   JaxbUtil jaxbUtil,
                   @NetworkJobThreadPool ExecutorWrapperService<ListeningExecutorService> networkJobExecutor) {
        this.subscriptionManagerPath = subscriptionManagerPath;
        this.maxExpires = maxExpires;
        this.httpUriBuilder = httpUriBuilder;
        this.soapUtil = soapUtil;
        this.wseFactory = wseFactory;
        this.historyObjectFactory = historyObjectFactory;
        this.wsaUtil = wsaUtil;
        this.subscriptionManagerFactory = subscriptionManagerFactory;
        this.soapFaultFactory = soapFaultFactory;
        this.jaxbUtil = jaxbUtil;
        this.networkJobExecutor = networkJobExecutor;
        mdibRevisionObserver = mdibRevisionObserverFactory.createMdibRevisionObserver(this, mdibAccess);
        mdibRevisionObserver.createInitialReport(mdibAccess.getMdibVersion());
        mdibAccess.registerObserver(mdibRevisionObserver);
    }

    @Override
    public void subscribe(RequestResponseObject requestResponseObject) throws SoapFaultException {
        Subscribe subscribe = soapUtil.getBody(requestResponseObject.getRequest(), Subscribe.class)
                .orElseThrow(getSoapFaultExceptionSupplier("Failed to parse subscribe request body."));

        var filter = Optional.ofNullable(subscribe.getFilter())
                .orElseThrow(getSoapFaultExceptionSupplier("No filter given, but required."));
        var filterContent = Optional.ofNullable(filter.getContent())
                .orElseThrow(getSoapFaultExceptionSupplier("No filter content given, but required."));
        var query = jaxbUtil.extractElement(filterContent.get(0), HistoryQueryType.class)
                .orElseThrow(getSoapFaultExceptionSupplier("Failed to parse historical data subscription filter."));
        if (!DpwsConstants.WS_DIALECT_HISTORY_SERVICE.equals(filter.getDialect())) {
            throw getSoapFaultExceptionSupplier("Dialect not supported by History service.").get();
        }

        var subscriptionManager = createSubscriptionManager(subscribe,
                requestResponseObject.getCommunicationContext().getTransportInfo());

        buildResponse(requestResponseObject, subscriptionManager.getSubscriptionManagerEpr());

        queryHistoricalData(query, subscriptionManager);
    }

    private void buildResponse(RequestResponseObject requestResponseObject,
                               EndpointReferenceType subscriptionManagerEpr) {
        SubscribeResponse subscribeResponse = wseFactory.createSubscribeResponse();
        subscribeResponse.setSubscriptionManager(subscriptionManagerEpr);
        subscribeResponse.setExpires(maxExpires);
        soapUtil.setWsaAction(requestResponseObject.getResponse(), WsEventingConstants.WSA_ACTION_SUBSCRIBE_RESPONSE);
        soapUtil.setBody(subscribeResponse, requestResponseObject.getResponse());
    }

    private SourceSubscriptionManager createSubscriptionManager(Subscribe subscribe, TransportInfo transportInfo) throws SoapFaultException {
        EndpointReferenceType epr = createSubscriptionManagerEpr(
                transportInfo.getScheme(),
                transportInfo.getLocalAddress().orElseThrow(() ->
                        new RuntimeException("Fatal error. Missing local address in transport information.")),
                transportInfo.getLocalPort().orElseThrow(() ->
                        new RuntimeException("Fatal error. Missing local port in transport information."))
        );

        EndpointReferenceType notifyTo = jaxbUtil.extractElement(subscribe.getDelivery().getContent().get(0),
                WsEventingConstants.NOTIFY_TO, EndpointReferenceType.class).orElseThrow(
                getSoapFaultExceptionSupplier("No 'NotifyTo' given, but required."));

        var subMan = subscriptionManagerFactory.createSourceSubscriptionManager(
                epr,
                maxExpires,
                notifyTo,
                subscribe.getEndTo(),
                epr.getAddress().getValue(),
                Collections.emptyList());

        subMan.startAsync().awaitRunning();

        return subMan;
    }

    private EndpointReferenceType createSubscriptionManagerEpr(String scheme,
                                                               String address,
                                                               Integer port) {
        var hostPart = httpUriBuilder.buildUri(scheme, address, port);
        var contextPath = "/" + UUID.randomUUID() + "/" + subscriptionManagerPath;
        var eprAddress = hostPart + contextPath;
        return wsaUtil.createEprWithAddress(eprAddress);
    }

    private void queryHistoricalData(HistoryQueryType query, SourceSubscriptionManager subscriptionManager) {
        networkJobExecutor.get().submit(() -> {
            // get historical data report
            var changeSequenceReportType = mdibRevisionObserver.getChangeSequenceReport(query);
            // send report
            sendNotification(subscriptionManager, changeSequenceReportType);

            // end subscription and shutdown subscription manager
            endSubscription(subscriptionManager);
        });
    }

    private void sendNotification(SourceSubscriptionManager subscriptionManager,
                                  ChangeSequenceReportType changeSequenceReportType) {
        EndpointReferenceType notifyTo = subscriptionManager.getNotifyTo();
        String wsaTo = wsaUtil.getAddressUri(notifyTo).orElseThrow(() ->
                new RuntimeException("Could not resolve URI from NotifyTo"));
        var message = soapUtil.createMessage(ACTION_HISTORY_MDIB_REPORT, wsaTo,
                historyObjectFactory.createChangeSequenceReport(changeSequenceReportType));
        subscriptionManager.offerNotification(new Notification(message));
    }

    private void endSubscription(SourceSubscriptionManager subscriptionManager) {
        if (subscriptionManager.getEndTo().isPresent()) {
            var endToMessage = createForEndTo(subscriptionManager, subscriptionManager.getEndTo().get());
            subscriptionManager.sendToEndTo(endToMessage);
        }
        subscriptionManager.stopAsync().awaitTerminated();
    }

    private SoapMessage createForEndTo(SourceSubscriptionManager subMan, EndpointReferenceType endTo) {
        SubscriptionEnd subscriptionEnd = wseFactory.createSubscriptionEnd();
        subscriptionEnd.setSubscriptionManager(subMan.getSubscriptionManagerEpr());
        subscriptionEnd.setStatus(WsEventingStatus.STATUS_SOURCE_CANCELLING.getUri());
        String wsaTo = wsaUtil.getAddressUri(endTo).orElse(null);

        SoapMessage msg = soapUtil.createMessage(WsEventingConstants.WSA_ACTION_SUBSCRIPTION_END, subscriptionEnd);
        Optional.ofNullable(wsaTo).ifPresent(to ->
                msg.getWsAddressingHeader().setTo(wsaUtil.createAttributedURIType(to)));

        return msg;
    }

    private Supplier<SoapFaultException> getSoapFaultExceptionSupplier(String msg) {
        return () -> new SoapFaultException(soapFaultFactory.createSenderFault(msg));
    }
}
