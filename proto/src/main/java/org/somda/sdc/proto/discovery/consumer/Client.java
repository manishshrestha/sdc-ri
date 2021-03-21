package org.somda.sdc.proto.discovery.consumer;

import com.google.common.collect.EvictingQueue;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.Service;
import com.google.inject.name.Named;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.util.AutoLock;
import org.somda.sdc.common.util.ExecutorWrapperService;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryConfig;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryConstants;
import org.somda.sdc.dpws.udp.UdpMessage;
import org.somda.sdc.dpws.udp.UdpMessageQueueObserver;
import org.somda.sdc.proto.addressing.AddressingUtil;
import org.somda.sdc.proto.discovery.common.UdpUtil;
import org.somda.sdc.proto.discovery.consumer.event.DeviceEnteredMessage;
import org.somda.sdc.proto.discovery.consumer.helper.ProbeCallable;
import org.somda.sdc.proto.discovery.consumer.helper.ResolveCallable;
import org.somda.sdc.proto.guice.ProtoDiscovery;
import org.somda.protosdc.proto.model.addressing.EndpointReference;
import org.somda.protosdc.proto.model.discovery.DiscoveryUdpMessage;
import org.somda.protosdc.proto.model.discovery.Endpoint;
import org.somda.protosdc.proto.model.discovery.Probe;
import org.somda.protosdc.proto.model.discovery.ProbeMatches;
import org.somda.protosdc.proto.model.discovery.Resolve;
import org.somda.protosdc.proto.model.discovery.ResolveMatches;
import org.somda.protosdc.proto.model.discovery.ScopeMatcher;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@SuppressWarnings("UnstableApiUsage")
public class Client extends AbstractIdleService implements Service, UdpMessageQueueObserver {
    private static final Logger LOG = LogManager.getLogger(Client.class);
    private static final AtomicInteger discoveryIdCounter = new AtomicInteger(0);
    private final Duration maxWaitForProbeMatches;
    private final Duration maxWaitForResolveMatches;
    private final UdpUtil udpUtil;
    private final ExecutorWrapperService<ListeningExecutorService> executorService;
    private final AddressingUtil addressingUtil;
    private final EventBus helloByeProbeEventBus;
    private final EvictingQueue<DiscoveryUdpMessage> messageBuffer;

    private final Lock probeLock;
    private final Lock resolveLock;

    private final Condition probeCondition;
    private final Condition resolveCondition;

    @Inject
    Client(@Named(WsDiscoveryConfig.MAX_WAIT_FOR_PROBE_MATCHES) Duration maxWaitForProbeMatches,
           @Named(WsDiscoveryConfig.MAX_WAIT_FOR_RESOLVE_MATCHES) Duration maxWaitForResolveMatches,
           @Named(WsDiscoveryConfig.PROBE_MATCHES_BUFFER_SIZE) Integer messageBufferSize,
           @ProtoDiscovery UdpUtil udpUtil,
           @ProtoDiscovery ExecutorWrapperService<ListeningExecutorService> executorService,
           AddressingUtil addressingUtil,
           EventBus helloByeProbeEventBus) {
        this.maxWaitForProbeMatches = maxWaitForProbeMatches;
        this.maxWaitForResolveMatches = maxWaitForResolveMatches;
        this.udpUtil = udpUtil;
        this.executorService = executorService;
        this.addressingUtil = addressingUtil;
        this.helloByeProbeEventBus = helloByeProbeEventBus;

        this.probeLock = new ReentrantLock();
        this.resolveLock = new ReentrantLock();

        this.probeCondition = probeLock.newCondition();
        this.resolveCondition = resolveLock.newCondition();

        this.messageBuffer = EvictingQueue.create(messageBufferSize);

        udpUtil.registerObserver(this);
    }

    @Subscribe
    void receiveUdpMessage(UdpMessage udpMessage) throws IOException {
        try {
            var discoveryMessage = DiscoveryUdpMessage.parseFrom(
                    new ByteArrayInputStream(udpMessage.getData(), 0, udpMessage.getLength()));

            switch (discoveryMessage.getTypeCase()) {
                case HELLO:
                    helloByeProbeEventBus.post(new DeviceEnteredMessage(discoveryMessage.getHello().getEndpoint()));
                    break;
                case PROBE_MATCHES:
                    try (var ignored = AutoLock.lock(probeLock)) {
                        messageBuffer.add(discoveryMessage);
                        probeCondition.signal();
                    }
                    break;
                case RESOLVE_MATCHES:
                    try (var ignored = AutoLock.lock(resolveLock)) {
                        messageBuffer.add(discoveryMessage);
                        resolveCondition.signal();
                    }
                    break;
                default:
                    // ignore
            }
        } catch (InvalidProtocolBufferException e) {
            LOG.debug("Protocol Buffers message processing error: {}", e.getMessage());
            LOG.trace("Protocol Buffers message processing error: {}", e.getMessage(), e);
        }
    }

    public void registerObserver(DiscoveryObserver observer) {
        helloByeProbeEventBus.register(observer);
    }

    public void unregisterObserver(DiscoveryObserver observer) {
        helloByeProbeEventBus.unregister(observer);
    }

    public ListenableFuture<List<Endpoint>> probe(ScopeMatcher scopeMatcher, int maxResults) {
        var probeId = String.format("probeId(%s@%s)", discoveryIdCounter.incrementAndGet(), this);
        var probe = Probe.newBuilder()
                .setScopesMatcher(scopeMatcher).build();
        var addressing = addressingUtil.assembleAddressing(
                WsDiscoveryConstants.WSA_ACTION_PROBE,
                WsDiscoveryConstants.WSA_UDP_TO);
        var future = executorService.get().submit(
                new ProbeCallable(
                        probeId,
                        maxResults,
                        maxWaitForProbeMatches,
                        probeLock,
                        probeCondition,
                        helloByeProbeEventBus,
                        () -> popProbeMatches(addressing.getMessageId())));

        udpUtil.sendMulticast(DiscoveryUdpMessage.newBuilder()
                .setAddressing(addressing)
                .setProbe(probe).build());
        return future;
    }

    public ListenableFuture<Endpoint> resolve(String eprAddress) {
        var resolve = Resolve.newBuilder()
                .setEndpointReference(EndpointReference.newBuilder().setAddress(eprAddress).build())
                .build();
        var addressing = addressingUtil.assembleAddressing(
                WsDiscoveryConstants.WSA_ACTION_RESOLVE,
                WsDiscoveryConstants.WSA_UDP_TO);
        var future = executorService.get().submit(
                new ResolveCallable(
                        maxWaitForResolveMatches,
                        addressing.getMessageId(),
                        resolveLock,
                        resolveCondition,
                        () -> popResolveMatches(addressing.getMessageId())));

        udpUtil.sendMulticast(DiscoveryUdpMessage.newBuilder()
                .setAddressing(addressing)
                .setResolve(resolve).build());
        return future;
    }

    private Optional<ProbeMatches> popProbeMatches(String messageId) {
        return popMessage(messageId)
                .filter(it -> it.getTypeCase() == DiscoveryUdpMessage.TypeCase.PROBE_MATCHES)
                .map(DiscoveryUdpMessage::getProbeMatches);
    }

    private Optional<ResolveMatches> popResolveMatches(String messageId) {
        return popMessage(messageId)
                .filter(it -> it.getTypeCase() == DiscoveryUdpMessage.TypeCase.RESOLVE_MATCHES)
                .map(DiscoveryUdpMessage::getResolveMatches);
    }

    private Optional<DiscoveryUdpMessage> popMessage(String messageId) {
        var msg = messageBuffer.stream().filter(message ->
                messageId.equals(message.getAddressing().getRelatesId())).findFirst();
        msg.ifPresent(messageBuffer::remove);
        return msg;
    }

    @Override
    protected void startUp() throws Exception {
        udpUtil.registerObserver(this);
        executorService.startAsync().awaitRunning();
    }

    @Override
    protected void shutDown() throws Exception {
        executorService.stopAsync().awaitTerminated();
        udpUtil.unregisterObserver(this);
    }
}
