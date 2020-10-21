package org.somda.sdc.dpws.soap.wsdiscovery;

import com.google.common.collect.EvictingQueue;
import com.google.common.eventbus.EventBus;
import com.google.common.primitives.UnsignedInteger;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.util.ExecutorWrapperService;
import org.somda.sdc.dpws.factory.TransportBindingFactory;
import org.somda.sdc.dpws.guice.WsDiscovery;
import org.somda.sdc.dpws.soap.NotificationSource;
import org.somda.sdc.dpws.soap.RequestResponseClient;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.factory.RequestResponseClientFactory;
import org.somda.sdc.dpws.soap.interception.InterceptorException;
import org.somda.sdc.dpws.soap.interception.MessageInterceptor;
import org.somda.sdc.dpws.soap.interception.NotificationObject;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingConstants;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.AttributedURIType;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.dpws.soap.wsdiscovery.event.ByeMessage;
import org.somda.sdc.dpws.soap.wsdiscovery.event.HelloMessage;
import org.somda.sdc.dpws.soap.wsdiscovery.event.ProbeMatchesMessage;
import org.somda.sdc.dpws.soap.wsdiscovery.event.ProbeTimeoutMessage;
import org.somda.sdc.dpws.soap.wsdiscovery.model.AppSequenceType;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ByeType;
import org.somda.sdc.dpws.soap.wsdiscovery.model.HelloType;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ObjectFactory;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ProbeMatchesType;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ProbeType;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ResolveMatchesType;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ResolveType;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ScopesType;

import javax.xml.namespace.QName;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.System.currentTimeMillis;

/**
 * Default implementation of {@linkplain WsDiscoveryClient}.
 */
public class WsDiscoveryClientInterceptor implements WsDiscoveryClient {
    private static final Logger LOG = LogManager.getLogger();

    private final Duration maxWaitForProbeMatches;
    private final Duration maxWaitForResolveMatches;
    private final Integer probeMatchesBufferSize;
    private final Integer resolveMatchesBufferSize;
    private final WsDiscoveryUtil wsdUtil;
    private final UnsignedInteger instanceId;
    private final ExecutorWrapperService<ListeningExecutorService> executorService;
    private final NotificationSource notificationSource;
    private final ObjectFactory wsdFactory;
    private final SoapUtil soapUtil;

    private EvictingQueue<SoapMessage> probeMatchesBuffer;
    private EvictingQueue<SoapMessage> resolveMatchesBuffer;

    private final Lock probeLock;
    private final Lock resolveLock;

    private final Condition probeCondition;
    private final Condition resolveCondition;

    private final EventBus helloByeProbeEvents;
    private final WsAddressingUtil wsaUtil;

    private final RequestResponseClient discoveryProxyClient;

    @AssistedInject
    WsDiscoveryClientInterceptor(@Assisted NotificationSource notificationSource,
                                 @Named(WsDiscoveryConfig.MAX_WAIT_FOR_PROBE_MATCHES) Duration maxWaitForProbeMatches,
                                 @Named(WsDiscoveryConfig.MAX_WAIT_FOR_RESOLVE_MATCHES)
                                         Duration maxWaitForResolveMatches,
                                 @Named(WsDiscoveryConfig.PROBE_MATCHES_BUFFER_SIZE) Integer probeMatchesBufferSize,
                                 @Named(WsDiscoveryConfig.RESOLVE_MATCHES_BUFFER_SIZE) Integer resolveMatchesBufferSize,
                                 @Named(WsDiscoveryConfig.DISCOVERY_PROXY_HOST) String discoveryProxyHost,
                                 WsDiscoveryUtil wsdUtil,
                                 @WsDiscovery ExecutorWrapperService<ListeningExecutorService> executorService,
                                 ObjectFactory wsdFactory,
                                 SoapUtil soapUtil,
                                 EventBus helloByeProbeEvents,
                                 WsAddressingUtil wsaUtil,
                                 TransportBindingFactory transportBindingFactory,
                                 RequestResponseClientFactory requestResponseClientFactory) {
        this.maxWaitForProbeMatches = maxWaitForProbeMatches;
        this.maxWaitForResolveMatches = maxWaitForResolveMatches;
        this.probeMatchesBufferSize = probeMatchesBufferSize;
        this.resolveMatchesBufferSize = resolveMatchesBufferSize;
        this.wsdUtil = wsdUtil;
        this.executorService = executorService;
        this.notificationSource = notificationSource;
        this.wsdFactory = wsdFactory;
        this.soapUtil = soapUtil;
        this.helloByeProbeEvents = helloByeProbeEvents;
        this.wsaUtil = wsaUtil;

        instanceId = UnsignedInteger.valueOf(currentTimeMillis() / 1000L);

        probeLock = new ReentrantLock();
        resolveLock = new ReentrantLock();

        probeCondition = probeLock.newCondition();
        resolveCondition = resolveLock.newCondition();

        if (discoveryProxyHost.isEmpty()) {
            discoveryProxyClient = null;
        } else {
            var tBinding = transportBindingFactory.createTransportBinding(discoveryProxyHost);
            discoveryProxyClient = requestResponseClientFactory.createRequestResponseClient(tBinding);
        }
    }

    @MessageInterceptor(WsDiscoveryConstants.WSA_ACTION_PROBE)
    void processProbe(NotificationObject nObj) {
        AppSequenceType appSequence = wsdUtil.createAppSequence(instanceId);
        nObj.getNotification().getWsDiscoveryHeader().setAppSequence(appSequence);
    }

    @MessageInterceptor(WsDiscoveryConstants.WSA_ACTION_RESOLVE)
    void processResolve(NotificationObject nObj) {
        nObj.getNotification().getWsDiscoveryHeader().setAppSequence(wsdUtil.createAppSequence(instanceId));
    }

    @MessageInterceptor(WsDiscoveryConstants.WSA_ACTION_PROBE_MATCHES)
    void processProbeMatches(NotificationObject nObj) {
        try {
            probeLock.lock();
            getProbeMatchesBuffer().add(nObj.getNotification());
            probeCondition.signal();
        } finally {
            probeLock.unlock();
        }
    }

    @MessageInterceptor(WsDiscoveryConstants.WSA_ACTION_RESOLVE_MATCHES)
    void processResolveMatches(NotificationObject nObj) {
        try {
            resolveLock.lock();
            getResolveMatchesBuffer().add(nObj.getNotification());
            resolveCondition.signal();
        } finally {
            resolveLock.unlock();
        }
    }

    @MessageInterceptor(WsDiscoveryConstants.WSA_ACTION_HELLO)
    void processHello(NotificationObject nObj) {
        Optional<HelloType> body = soapUtil.getBody(nObj.getNotification(), HelloType.class);
        body.ifPresent(helloType -> helloByeProbeEvents.post(new HelloMessage(helloType)));
    }

    @MessageInterceptor(WsDiscoveryConstants.WSA_ACTION_BYE)
    void processBye(NotificationObject nObj) {
        Optional<ByeType> body = soapUtil.getBody(nObj.getNotification(), ByeType.class);
        body.ifPresent(byeType -> helloByeProbeEvents.post(new ByeMessage(byeType)));
    }

    @Override
    public ListenableFuture<ProbeMatchesType> sendDirectedProbe(RequestResponseClient rrClient,
                                                                List<QName> types,
                                                                List<String> scopes) {
        return executorService.get().submit(() -> {
            SoapMessage response = rrClient.sendRequestResponse(createProbeMessage(types, scopes));
            return soapUtil.getBody(response, ProbeMatchesType.class)
                    .orElseThrow(() -> new RuntimeException("SOAP message body malformed"));
        });
    }

    @Override
    public ListenableFuture<Integer> sendProbe(String probeId, Collection<QName> types,
                                               Collection<String> scopes, Integer maxResults)
            throws MarshallingException, TransportException, InterceptorException {
        SoapMessage probeMsg = createProbeMessage(types, scopes);

        var msgIdUri = soapUtil.createUriFromUuid(UUID.randomUUID());
        AttributedURIType msgId = wsaUtil.createAttributedURIType(msgIdUri);
        probeMsg.getWsAddressingHeader().setMessageId(msgId);

        if (discoveryProxyClient != null) {
            var future = sendDirectedProbe(discoveryProxyClient, new ArrayList<>(types), new ArrayList<>(scopes));
            try {
                var probeMatchesType = future.get(50000, TimeUnit.SECONDS);
                helloByeProbeEvents.post(new ProbeMatchesMessage(probeId, probeMatchesType));
                var result = SettableFuture.<Integer>create();
                result.set(1);
                return result;
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                LOG.error("Probe to discovery proxy failed; fallback to multicast", e);
            }
        }

        ListenableFuture<Integer> future = executorService.get().submit(new ProbeRunnable(probeId, maxResults,
                maxWaitForProbeMatches, msgIdUri, probeLock, probeCondition, getProbeMatchesBuffer(),
                soapUtil, helloByeProbeEvents));

        notificationSource.sendNotification(probeMsg);

        return future;
    }

    @Override
    public ListenableFuture<Integer> sendProbe(String probeId, Collection<QName> types, Collection<String> scopes)
            throws MarshallingException, TransportException, InterceptorException {
        return sendProbe(probeId, types, scopes, Integer.MAX_VALUE);
    }

    @Override
    public ListenableFuture<ResolveMatchesType> sendResolve(EndpointReferenceType epr)
            throws MarshallingException, TransportException, InterceptorException {
        ResolveType resolveType = wsdFactory.createResolveType();
        resolveType.setEndpointReference(epr);

        SoapMessage resolveMsg = soapUtil.createMessage(WsDiscoveryConstants.WSA_ACTION_RESOLVE,
                WsDiscoveryConstants.WSA_UDP_TO, wsdFactory.createResolve(resolveType));

        var msgIdUri = soapUtil.createUriFromUuid(UUID.randomUUID());
        AttributedURIType msgId = wsaUtil.createAttributedURIType(msgIdUri);
        resolveMsg.getWsAddressingHeader().setMessageId(msgId);

        ListenableFuture<ResolveMatchesType> future = executorService.get().submit(
                new ResolveCallable(maxWaitForResolveMatches, msgIdUri, resolveLock, resolveCondition,
                        getResolveMatchesBuffer(), soapUtil));

        notificationSource.sendNotification(resolveMsg);

        return future;
    }

    private SoapMessage createProbeMessage(Collection<QName> types, Collection<String> scopes) {
        ProbeType probeType = wsdFactory.createProbeType();
        probeType.setTypes(new ArrayList<>(types));
        ScopesType scopesType = wsdFactory.createScopesType();
        // Always create RFC3986 by default
        // See http://docs.oasis-open.org/ws-dd/discovery/1.1/os/wsdd-discovery-1.1-spec-os.html#_Toc234231831
        scopesType.setMatchBy(MatchBy.RFC3986.getUri());
        scopesType.setValue(new ArrayList<>(scopes));
        probeType.setScopes(scopesType);

        return soapUtil.createMessage(WsDiscoveryConstants.WSA_ACTION_PROBE,
                WsDiscoveryConstants.WSA_UDP_TO, wsdFactory.createProbe(probeType));
    }

    private synchronized EvictingQueue<SoapMessage> getProbeMatchesBuffer() {
        if (probeMatchesBuffer == null) {
            probeMatchesBuffer = EvictingQueue.create(probeMatchesBufferSize);
        }
        return probeMatchesBuffer;
    }

    private synchronized EvictingQueue<SoapMessage> getResolveMatchesBuffer() {
        if (resolveMatchesBuffer == null) {
            resolveMatchesBuffer = EvictingQueue.create(resolveMatchesBufferSize);
        }
        return resolveMatchesBuffer;
    }

    private Optional<SoapMessage> popMatches(EvictingQueue<SoapMessage> messageQueue, String messageId) {
        Optional<SoapMessage> item = messageQueue.stream().filter(message ->
                messageId.equals(message.getWsAddressingHeader().getRelatesTo().orElse(wsaUtil.createRelatesToType(
                        WsAddressingConstants.UNSPECIFIED_MESSAGE))
                        .getValue())).findFirst();
        item.ifPresent(messageQueue::remove);
        return item;
    }

    private class ResolveCallable implements Callable<ResolveMatchesType> {
        private final String wsaRelatesTo;
        private final SoapUtil soapUtil;
        private final Lock lock;
        private final long maxWaitInMillis;
        private final Condition condition;
        private final EvictingQueue<SoapMessage> messageQueue;

        public ResolveCallable(Duration maxWait,
                               String wsaRelatesTo,
                               Lock lock,
                               Condition condition,
                               EvictingQueue<SoapMessage> messageQueue,
                               SoapUtil soapUtil) {
            this.maxWaitInMillis = maxWait.toMillis();
            this.messageQueue = messageQueue;
            this.wsaRelatesTo = wsaRelatesTo;
            this.lock = lock;
            this.condition = condition;
            this.soapUtil = soapUtil;
        }

        @Override
        public ResolveMatchesType call() throws Exception {
            try {
                lock.lock();
                long wait = maxWaitInMillis;
                while (wait > 0) {
                    long tStartInMillis = System.currentTimeMillis();
                    Optional<SoapMessage> msg = popMatches(messageQueue, wsaRelatesTo);
                    if (msg.isPresent()) {
                        return soapUtil.getBody(msg.get(), ResolveMatchesType.class).orElseThrow(() ->
                                new RuntimeException("SOAP message body malformed"));
                    }

                    if (!condition.await(wait, TimeUnit.MILLISECONDS)) {
                        break;
                    }

                    msg = popMatches(messageQueue, wsaRelatesTo);
                    wait -= System.currentTimeMillis() - tStartInMillis;
                    if (msg.isPresent()) {
                        return soapUtil.getBody(msg.get(), ResolveMatchesType.class).orElseThrow(() ->
                                new RuntimeException("SOAP message body malformed"));
                    }
                }
            } finally {
                lock.unlock();
            }

            throw new RuntimeException(String.format(
                    "No ResolveMatches message received in %s milliseconds, Resolve MessageID was %s",
                    Long.valueOf(maxWaitInMillis).toString(),
                    wsaRelatesTo
            ));
        }
    }

    private class ProbeRunnable implements Callable<Integer> {
        private final String wsaRelatesTo;
        private final SoapUtil soapUtil;
        private final EventBus helloByeProbeEvents;
        private final Lock lock;
        private final String probeId;
        private final Integer maxResults;
        private final long maxWaitInMillis;
        private final Condition condition;
        private final EvictingQueue<SoapMessage> messageQueue;


        public ProbeRunnable(String probeId,
                             Integer maxResults,
                             Duration maxWait,
                             String wsaRelatesTo,
                             Lock lock,
                             Condition condition,
                             EvictingQueue<SoapMessage> messageQueue,
                             SoapUtil soapUtil,
                             EventBus helloByeProbeEvents) {
            this.probeId = probeId;
            this.maxResults = maxResults;
            this.maxWaitInMillis = maxWait.toMillis();
            this.messageQueue = messageQueue;
            this.wsaRelatesTo = wsaRelatesTo;
            this.lock = lock;
            this.condition = condition;
            this.soapUtil = soapUtil;
            this.helloByeProbeEvents = helloByeProbeEvents;
        }

        @Override
        public Integer call() throws Exception {
            Integer probeMatchesCount = 0;
            long wait = maxWaitInMillis;
            try {
                lock.lock();
                while (wait > 0) {
                    long tStartInMillis = System.currentTimeMillis();
                    probeMatchesCount = fetchData(probeMatchesCount);
                    if (probeMatchesCount.equals(maxResults)) {
                        break;
                    }

                    if (!condition.await(wait, TimeUnit.MILLISECONDS)) {
                        break;
                    }

                    wait -= System.currentTimeMillis() - tStartInMillis;
                    probeMatchesCount = fetchData(probeMatchesCount);
                    if (probeMatchesCount.equals(maxResults)) {
                        break;
                    }
                }
            } finally {
                lock.unlock();
            }

            helloByeProbeEvents.post(new ProbeTimeoutMessage(probeMatchesCount, probeId));
            return probeMatchesCount;
        }

        private Integer fetchData(Integer probeMatchesCount) {
            var copyProbeMatchesCount = probeMatchesCount;
            Optional<SoapMessage> msg = popMatches(messageQueue, wsaRelatesTo);
            if (msg.isPresent()) {
                ProbeMatchesType pMatches = soapUtil.getBody(msg.get(), ProbeMatchesType.class)
                        .orElseThrow(() -> new RuntimeException("SOAP message body malformed"));
                helloByeProbeEvents.post(new ProbeMatchesMessage(probeId, pMatches));
                copyProbeMatchesCount++;
            }
            return copyProbeMatchesCount;
        }
    }

    @Override
    public void registerHelloByeAndProbeMatchesObserver(HelloByeAndProbeMatchesObserver observer) {
        helloByeProbeEvents.register(observer);
    }

    @Override
    public void unregisterHelloByeAndProbeMatchesObserver(HelloByeAndProbeMatchesObserver observer) {
        helloByeProbeEvents.unregister(observer);
    }
}
