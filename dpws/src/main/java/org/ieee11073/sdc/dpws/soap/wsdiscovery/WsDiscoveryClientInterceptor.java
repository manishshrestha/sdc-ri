package org.ieee11073.sdc.dpws.soap.wsdiscovery;

import com.google.common.collect.EvictingQueue;
import com.google.common.eventbus.EventBus;
import com.google.common.primitives.UnsignedInteger;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import org.ieee11073.sdc.dpws.guice.WsDiscovery;
import org.ieee11073.sdc.dpws.soap.NotificationSource;
import org.ieee11073.sdc.dpws.soap.RequestResponseClient;
import org.ieee11073.sdc.dpws.soap.SoapMessage;
import org.ieee11073.sdc.dpws.soap.SoapUtil;
import org.ieee11073.sdc.dpws.soap.exception.MarshallingException;
import org.ieee11073.sdc.dpws.soap.exception.TransportException;
import org.ieee11073.sdc.dpws.soap.interception.InterceptorResult;
import org.ieee11073.sdc.dpws.soap.interception.MessageInterceptor;
import org.ieee11073.sdc.dpws.soap.interception.NotificationObject;
import org.ieee11073.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.*;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.event.ByeMessage;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.event.HelloMessage;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.event.ProbeMatchesMessage;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.event.ProbeTimeoutMessage;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.model.ObjectFactory;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.model.*;

import javax.xml.namespace.QName;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.System.currentTimeMillis;

/**
 * Default implementation of {@linkplain WsDiscoveryClient}.
 */
public class WsDiscoveryClientInterceptor implements WsDiscoveryClient {

    private final Duration maxWaitForProbeMatches;
    private final Duration maxWaitForResolveMatches;
    private final Integer probeMatchesBufferSize;
    private final Integer resolveMatchesBufferSize;
    private final WsDiscoveryUtil wsdUtil;
    private final UnsignedInteger instanceId;
    private final ListeningExecutorService executorService;
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

    @AssistedInject
    WsDiscoveryClientInterceptor(@Assisted NotificationSource notificationSource,
                                 @Named(WsDiscoveryConfig.MAX_WAIT_FOR_PROBE_MATCHES) Duration maxWaitForProbeMatches,
                                 @Named(WsDiscoveryConfig.MAX_WAIT_FOR_RESOLVE_MATCHES) Duration maxWaitForResolveMatches,
                                 @Named(WsDiscoveryConfig.PROBE_MATCHES_BUFFER_SIZE) Integer probeMatchesBufferSize,
                                 @Named(WsDiscoveryConfig.RESOLVE_MATCHES_BUFFER_SIZE) Integer resolveMatchesBufferSize,
                                 WsDiscoveryUtil wsdUtil,
                                 @WsDiscovery ExecutorService executorService,
                                 ObjectFactory wsdFactory,
                                 SoapUtil soapUtil,
                                 EventBus helloByeProbeEvents,
                                 WsAddressingUtil wsaUtil) {
        this.maxWaitForProbeMatches = maxWaitForProbeMatches;
        this.maxWaitForResolveMatches = maxWaitForResolveMatches;
        this.probeMatchesBufferSize = probeMatchesBufferSize;
        this.resolveMatchesBufferSize = resolveMatchesBufferSize;
        this.wsdUtil = wsdUtil;
        this.executorService = MoreExecutors.listeningDecorator(executorService);
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
    }

    @MessageInterceptor(WsDiscoveryConstants.WSA_ACTION_PROBE)
    InterceptorResult processProbe(NotificationObject nObj) {
        AppSequenceType appSequence = wsdUtil.createAppSequence(instanceId);
        nObj.getNotification().getWsDiscoveryHeader().setAppSequence(appSequence);
        return InterceptorResult.PROCEED;
    }

    @MessageInterceptor(WsDiscoveryConstants.WSA_ACTION_RESOLVE)
    InterceptorResult processResolve(NotificationObject nObj) {
        nObj.getNotification().getWsDiscoveryHeader().setAppSequence(wsdUtil.createAppSequence(instanceId));
        return InterceptorResult.PROCEED;
    }

    @MessageInterceptor(WsDiscoveryConstants.WSA_ACTION_PROBE_MATCHES)
    InterceptorResult processProbeMatches(NotificationObject nObj) {
        try {
            probeLock.lock();
            getProbeMatchesBuffer().add(nObj.getNotification());
            probeCondition.signal();
        } finally {
            probeLock.unlock();
        }
        return InterceptorResult.PROCEED;
    }

    @MessageInterceptor(WsDiscoveryConstants.WSA_ACTION_RESOLVE_MATCHES)
    InterceptorResult processResolveMatches(NotificationObject nObj) {
        try {
            resolveLock.lock();
            getResolveMatchesBuffer().add(nObj.getNotification());
            resolveCondition.signal();
        } finally {
            resolveLock.unlock();
        }
        return InterceptorResult.PROCEED;
    }

    @MessageInterceptor(WsDiscoveryConstants.WSA_ACTION_HELLO)
    InterceptorResult processHello(NotificationObject nObj) {
        Optional<HelloType> body = soapUtil.getBody(nObj.getNotification(), HelloType.class);
        body.ifPresent(helloType -> helloByeProbeEvents.post(new HelloMessage(helloType)));
        return InterceptorResult.PROCEED;
    }

    @MessageInterceptor(WsDiscoveryConstants.WSA_ACTION_BYE)
    InterceptorResult processBye(NotificationObject nObj) {
        Optional<ByeType> body = soapUtil.getBody(nObj.getNotification(), ByeType.class);
        body.ifPresent(byeType -> helloByeProbeEvents.post(new ByeMessage(byeType)));
        return InterceptorResult.PROCEED;
    }

    @Override
    public ListenableFuture<ProbeMatchesType> sendDirectedProbe(RequestResponseClient rrClient,
                                                                List<QName> types,
                                                                List<String> scopes) {
        return executorService.submit(() -> {
            SoapMessage response = rrClient.sendRequestResponse(createProbeMessage(types, scopes));
            return soapUtil.getBody(response, ProbeMatchesType.class)
                    .orElseThrow(() -> new RuntimeException("SOAP message body malformed"));
        });
    }

    @Override
    public ListenableFuture<Integer> sendProbe(String probeId, List<QName> types, List<String> scopes, Integer maxResults)
            throws MarshallingException, TransportException {
        SoapMessage probeMsg = createProbeMessage(types, scopes);

        URI msgIdUri = soapUtil.createUriFromUuid(UUID.randomUUID());
        AttributedURIType msgId = wsaUtil.createAttributedURIType(msgIdUri);
        probeMsg.getWsAddressingHeader().setMessageId(msgId);

        ListenableFuture<Integer> future = executorService.submit(new ProbeRunnable(probeId, maxResults,
                maxWaitForProbeMatches, msgIdUri.toString(), probeLock, probeCondition, getProbeMatchesBuffer(),
                soapUtil, helloByeProbeEvents));

        notificationSource.sendNotification(probeMsg);

        return future;
    }

    @Override
    public ListenableFuture<Integer> sendProbe(String probeId, List<QName> types, List<String> scopes)
            throws MarshallingException, TransportException{
        return sendProbe(probeId, types, scopes, Integer.MAX_VALUE);
    }

    @Override
    public ListenableFuture<ResolveMatchesType> sendResolve(EndpointReferenceType epr)
            throws MarshallingException, TransportException {
        ResolveType resolveType = wsdFactory.createResolveType();
        resolveType.setEndpointReference(epr);

        SoapMessage resolveMsg = soapUtil.createMessage(WsDiscoveryConstants.WSA_ACTION_RESOLVE,
                WsDiscoveryConstants.WSA_UDP_TO, wsdFactory.createResolve(resolveType));

        URI msgIdUri = soapUtil.createUriFromUuid(UUID.randomUUID());
        AttributedURIType msgId = wsaUtil.createAttributedURIType(msgIdUri);
        resolveMsg.getWsAddressingHeader().setMessageId(msgId);

        ListenableFuture<ResolveMatchesType> future = executorService.submit(
                new ResolveCallable(maxWaitForResolveMatches, msgIdUri.toString(), resolveLock, resolveCondition,
                        getResolveMatchesBuffer(), soapUtil));

        notificationSource.sendNotification(resolveMsg);

        return future;
    }

    private SoapMessage createProbeMessage(List<QName> types, List<String> scopes) {
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
        Optional<SoapMessage> item = messageQueue.parallelStream().filter(message ->
                messageId.equals(message.getWsAddressingHeader().getRelatesTo().orElse(new AttributedURIType())
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

                    condition.await(wait, TimeUnit.MILLISECONDS);

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

            throw new RuntimeException(String.format("No ResolveMatches message received in %s milliseconds",
                    Long.valueOf(maxWaitInMillis).toString()));
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

                    condition.await(wait, TimeUnit.MILLISECONDS);

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
            Optional<SoapMessage> msg = popMatches(messageQueue, wsaRelatesTo);
            if (msg.isPresent()) {
                ProbeMatchesType pMatches = soapUtil.getBody(msg.get(), ProbeMatchesType.class)
                        .orElseThrow(() -> new RuntimeException("SOAP message body malformed"));
                helloByeProbeEvents.post(new ProbeMatchesMessage(probeId, pMatches));
                probeMatchesCount++;
            }
            return probeMatchesCount;
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
