package org.somda.sdc.proto.discovery.provider;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Service;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.dpws.soap.SoapConstants;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.wsdiscovery.MatchBy;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryConstants;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryUtil;
import org.somda.sdc.dpws.udp.UdpMessage;
import org.somda.sdc.dpws.udp.UdpMessageQueueObserver;
import org.somda.sdc.proto.addressing.AddressingUtil;
import org.somda.sdc.proto.addressing.AddressingValidator;
import org.somda.sdc.proto.addressing.MessageDuplicateDetection;
import org.somda.sdc.proto.addressing.ValidationException;
import org.somda.sdc.proto.addressing.factory.AddressingValidatorFactory;
import org.somda.sdc.proto.discovery.common.UdpUtil;
import org.somda.sdc.proto.model.addressing.Addressing;
import org.somda.sdc.proto.model.addressing.AddressingTypes;
import org.somda.sdc.proto.model.addressing.EndpointReference;
import org.somda.sdc.proto.model.discovery.AppSequence;
import org.somda.sdc.proto.model.discovery.DiscoveryMessages;
import org.somda.sdc.proto.model.discovery.DiscoveryTypes;
import org.somda.sdc.proto.model.discovery.DiscoveryUdpMessage;
import org.somda.sdc.proto.model.discovery.Endpoint;
import org.somda.sdc.proto.model.discovery.Hello;
import org.somda.sdc.proto.model.discovery.Probe;
import org.somda.sdc.proto.model.discovery.ProbeMatches;
import org.somda.sdc.proto.model.discovery.Resolve;
import org.somda.sdc.proto.model.discovery.ResolveMatches;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

public class TargetServiceImpl extends AbstractIdleService implements Service, TargetService {
    private static final Logger LOG = LogManager.getLogger(TargetServiceImpl.class);
    private final EndpointReference endpointReference;
    private final UdpUtil udpUtil;
    private final WsDiscoveryUtil wsdUtil;
    private final Collection<String> scopes;
    private final Collection<String> xAddrs;
    private final AddressingValidator addressingValidator;
    private final AddressingUtil addressingUtil;

    private long instanceId;
    private long messageIdCounter;
    private long currentMetadataVersion;

    @Inject
    TargetServiceImpl(@Assisted String eprAddress,
                      MessageDuplicateDetection messageDuplicateDetection,
                      UdpUtil udpUtil,
                      AddressingValidatorFactory addressingValidatorFactory,
                      AddressingUtil addressingUtil,
                      WsDiscoveryUtil wsdUtil) {
        this.addressingUtil = addressingUtil;
        this.addressingValidator = addressingValidatorFactory.create(messageDuplicateDetection);
        this.endpointReference = EndpointReference.newBuilder().setAddress(eprAddress).build();
        this.udpUtil = udpUtil;
        this.wsdUtil = wsdUtil;
        this.instanceId = System.currentTimeMillis() / 1000L;
        this.messageIdCounter = 0;
        this.currentMetadataVersion = 0;
        this.scopes = new ArrayList<>();
        this.xAddrs = new ArrayList<>();
    }

    @Override
    public synchronized void updateScopes(Collection<String> scopes) {
        this.scopes.clear();
        this.scopes.addAll(scopes);
    }

    @Override
    public synchronized void updateXAddrs(Collection<String> xAddrs) {
        this.xAddrs.clear();
        this.xAddrs.addAll(xAddrs);
    }

    @Subscribe
    void receiveUdpMessage(UdpMessage udpMessage) throws IOException {
        try {
            var discoveryMessage = DiscoveryUdpMessage.parseFrom(
                    new ByteArrayInputStream(udpMessage.getData(), 0, udpMessage.getLength()));

            switch (discoveryMessage.getTypeCase()) {
                case PROBE:
                    processProbe(udpMessage, discoveryMessage.getAddressing(), discoveryMessage.getProbe());
                    break;
                case RESOLVE:
                    processResolve(udpMessage, discoveryMessage.getAddressing(), discoveryMessage.getResolve());
                    break;
                default:
                    // ignore
            }
        } catch (FaultException e) {
            udpUtil.sendResponse(e.getFault(), udpMessage);
        } catch (ValidationException e) {
            // Ignore broken messages
        } catch (InvalidProtocolBufferException e) {
            LOG.debug("Protocol Buffers message processing error: {}", e.getMessage());
            LOG.trace("Protocol Buffers message processing error: {}", e.getMessage(), e);
        }
    }

    @Override
    protected void startUp() throws Exception {
        udpUtil.registerObserver(this);
        sendHello();
    }

    @Override
    protected void shutDown() throws Exception {
        // no bye as it would be sent over an unsecured channel
        udpUtil.unregisterObserver(this);
    }

    private synchronized void processProbe(UdpMessage requestMessage,
                                           Addressing addressing,
                                           Probe probe) throws FaultException, ValidationException {
        validateProbe(addressing);

        var probedScopes = probe.getScopesMatcher();
        var matchBy = Optional.ofNullable(probedScopes.getMatchBy()).orElse(MatchBy.RFC3986.getUri());
        if (matchBy.isEmpty()) {
            matchBy = MatchBy.RFC3986.getUri();
        }

        var finalMatchBy = matchBy;
        var matcher = Arrays.stream(MatchBy.values())
                .filter(item -> item.getUri().equals(finalMatchBy))
                .findAny().orElseThrow(() -> new FaultException(
                        SoapConstants.SENDER,
                        WsDiscoveryConstants.MATCHING_RULE_NOT_SUPPORTED,
                        "The matching rule specified is not supported."));

        var scopesSuperset = new ArrayList<>(scopes);
        if (!wsdUtil.isScopesMatching(scopesSuperset, probedScopes.getScopesList(), matcher)) {
            // scopes do not match: ignore request
            return;
        }

        var probeMatches = ProbeMatches.newBuilder()
                .addEndpoint(createEndpoint(getCurrentMetadataVersion()));

        udpUtil.sendResponse(DiscoveryUdpMessage.newBuilder()
                .setAddressing(addressingUtil.assemblyAddressing(
                        WsDiscoveryConstants.WSA_ACTION_PROBE_MATCHES,
                        WsDiscoveryConstants.WSA_UDP_TO,
                        addressing.getMessageId()))
                .setProbeMatches(
                        probeMatches.build()).build(), requestMessage);
    }

    private synchronized void processResolve(UdpMessage requestMessage,
                                             Addressing addressing,
                                             Resolve resolve) throws ValidationException {
        validateResolve(addressing);
        var eprToResolve = resolve.getEndpointReference();

        if (!URI.create(eprToResolve.getAddress()).equals(URI.create(endpointReference.getAddress()))) {
            // epr does not match, ignore request
            return;
        }

        var resolveMatches = ResolveMatches.newBuilder()
                .setEndpoint(createEndpoint(getCurrentMetadataVersion()));

        udpUtil.sendResponse(DiscoveryUdpMessage.newBuilder()
                .setAddressing(addressingUtil.assemblyAddressing(
                        WsDiscoveryConstants.WSA_ACTION_RESOLVE_MATCHES,
                        WsDiscoveryConstants.WSA_UDP_TO,
                        addressing.getMessageId()))
                .setResolveMatches(resolveMatches.build()).build(), requestMessage);
    }

    private void sendHello() {
        var hello = Hello.newBuilder()
                .setEndpoint(createEndpoint(setAndGetNextMetadataVersion()));

        udpUtil.sendMulticast(DiscoveryUdpMessage.newBuilder()
                .setAddressing(Addressing.newBuilder()
                        .setAction(WsDiscoveryConstants.WSA_ACTION_HELLO))
                .setAppSequence(nextAppSequence())
                .setHello(hello).build());
    }

    private synchronized long getCurrentMetadataVersion() {
        return currentMetadataVersion;
    }

    private synchronized long setAndGetNextMetadataVersion() {
        var newVersion = Instant.now().toEpochMilli() / 1000L;

        // If there is more than one metadata version increment within one second just increment the current version.
        // This approach does not scale if there are tons of changes to the metadata version in a short time.
        // For this unlikely scenario an implementation is required that can persist metadata versions
        if (newVersion <= currentMetadataVersion) {
            currentMetadataVersion++;
        } else {
            currentMetadataVersion = newVersion;
        }

        return currentMetadataVersion;
    }


    private synchronized AppSequence nextAppSequence() {
        return AppSequence.newBuilder()
                .setInstanceId(instanceId)
                .setMessageNumber(++messageIdCounter)
                .build();
    }

    private synchronized Endpoint createEndpoint(long metadataVersion) {
        return Endpoint.newBuilder()
                .setEndpointReference(endpointReference)
                .setMetadataVersion(metadataVersion)
                .addAllScope(scopes)
                .addAllXAddr(xAddrs)
                .build();
    }

    private void validateProbe(Addressing addressing) throws ValidationException {
        addressingValidator.validate(addressing)
                .validateMessageId()
                .validateAction(WsDiscoveryConstants.WSA_ACTION_PROBE);
    }

    private void validateResolve(Addressing addressing) throws ValidationException {
        addressingValidator.validate(addressing)
                .validateMessageId()
                .validateAction(WsDiscoveryConstants.WSA_ACTION_RESOLVE);
    }
}
