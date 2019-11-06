package org.somda.sdc.dpws.soap.wsdiscovery;

import com.google.common.primitives.UnsignedInteger;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.somda.sdc.dpws.soap.NotificationSource;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.factory.SoapFaultFactory;
import org.somda.sdc.dpws.soap.interception.*;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingHeader;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.dpws.soap.wsdiscovery.factory.WsDiscoveryFaultFactory;
import org.somda.sdc.common.util.ObjectUtilImpl;
import org.somda.sdc.dpws.soap.wsdiscovery.model.*;

import javax.annotation.Nullable;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Default implementation of the {@linkplain WsDiscoveryTargetService}.
 */
public class WsDiscoveryTargetServiceInterceptor implements WsDiscoveryTargetService {
    private final ObjectFactory wsdFactory;
    private final SoapFaultFactory soapFaultFactory;
    private final WsDiscoveryFaultFactory wsdFaultFactory;
    private final SoapUtil soapUtil;
    private final WsAddressingUtil wsaUtil;
    private final WsDiscoveryUtil wsdUtil;
    private final EndpointReferenceType targetServiceEpr;
    private final NotificationSource notificationSource;
    private final ObjectUtilImpl objectUtil;
    private final UnsignedInteger instanceId;
    private List<QName> types;
    private List<String> scopes;
    private List<String> xAddrs;
    private final AtomicBoolean metadataModified;
    private UnsignedInteger metadataVersion;
    private MatchBy matchBy;

    private final Lock lock;

    @AssistedInject
    WsDiscoveryTargetServiceInterceptor(@Assisted EndpointReferenceType targetServiceEpr,
                                        @Assisted NotificationSource notificationSource,
                                        ObjectFactory wsdFactory,
                                        SoapFaultFactory soapFaultFactory,
                                        SoapUtil soapUtil,
                                        WsDiscoveryFaultFactory wsdFaultFactory,
                                        WsAddressingUtil wsaUtil,
                                        WsDiscoveryUtil wsdUtil,
                                        ObjectUtilImpl objectUtil) {
        this.targetServiceEpr = targetServiceEpr;
        this.notificationSource = notificationSource;
        this.objectUtil = objectUtil;
        this.instanceId = UnsignedInteger.valueOf(System.currentTimeMillis() / 1000L);
        this.wsdFactory = wsdFactory;
        this.soapFaultFactory = soapFaultFactory;
        this.soapUtil = soapUtil;
        this.wsdFaultFactory = wsdFaultFactory;
        this.wsaUtil = wsaUtil;
        this.wsdUtil = wsdUtil;
        lock = new ReentrantLock();
        metadataModified = new AtomicBoolean(false);
        metadataVersion = getNewMetadataVersion(null);

        matchBy = MatchBy.RFC3986;

        types = new ArrayList<>();
        scopes = new ArrayList<>();
        xAddrs = new ArrayList<>();
    }

    @MessageInterceptor(value = WsDiscoveryConstants.WSA_ACTION_PROBE, direction = Direction.REQUEST)
    void processProbe(RequestResponseObject rrObj)
            throws SoapFaultException {
        SoapMessage inMsg = rrObj.getRequest();
        ProbeType probe = validateIncomingMessage(inMsg, ProbeType.class);
        ScopesType scopesFromProbe = Optional.ofNullable(probe.getScopes()).orElse(wsdFactory.createScopesType());
        List<QName> typesFromProbe = Optional.ofNullable(probe.getTypes()).orElse(new ArrayList<>());

        String matchBy = Optional.ofNullable(scopesFromProbe.getMatchBy()).orElse(MatchBy.RFC3986.getUri());
        MatchBy matcher = Arrays.asList(MatchBy.values()).parallelStream()
                .filter(item -> item.getUri().equals(matchBy))
                .findAny().orElseThrow(() -> new SoapFaultException(wsdFaultFactory.createMatchingRuleNotSupported()));

        EndpointReferenceType endpointReference = getEndpointReference();
        List<String> scopes = getScopes();
        List<QName> types = getTypes();
        List<String> xAddrs = getXAddrs();
        UnsignedInteger metadataVersion = getMetadataVersion();

        if (!wsdUtil.isScopesMatching(scopes, scopesFromProbe.getValue(), matcher) ||
                !wsdUtil.isTypesMatching(this.types, typesFromProbe)) {
            throw new RuntimeException("Scopes and Types do not match in incoming Probe.");
        }

        ProbeMatchType probeMatchType = wsdFactory.createProbeMatchType();
        probeMatchType.setEndpointReference(endpointReference);
        ScopesType scopesType = wsdFactory.createScopesType();
        scopesType.setValue(scopes);
        probeMatchType.setScopes(scopesType);
        probeMatchType.setTypes(types);
        probeMatchType.setXAddrs(xAddrs);
        probeMatchType.setMetadataVersion(metadataVersion.longValue());

        ProbeMatchesType probeMatchesType = wsdFactory.createProbeMatchesType();
        List<ProbeMatchType> probeMatchTypeList = new ArrayList<>();
        probeMatchTypeList.add(probeMatchType);
        probeMatchesType.setProbeMatch(probeMatchTypeList);
        SoapMessage probeMatchesMsg = rrObj.getResponse();
        WsAddressingHeader wsaHeader = probeMatchesMsg.getWsAddressingHeader();
        wsaHeader.setAction(wsaUtil.createAttributedURIType(WsDiscoveryConstants.WSA_ACTION_PROBE_MATCHES));
        wsaHeader.setTo(wsaUtil.createAttributedURIType(WsDiscoveryConstants.WSA_UDP_TO));
        probeMatchesMsg.getWsDiscoveryHeader().setAppSequence(wsdUtil.createAppSequence(instanceId));
        soapUtil.setBody(wsdFactory.createProbeMatches(probeMatchesType), probeMatchesMsg);
    }

    @MessageInterceptor(value = WsDiscoveryConstants.WSA_ACTION_RESOLVE, direction = Direction.REQUEST)
    void processResolve(RequestResponseObject rrObj)
            throws SoapFaultException {
        SoapMessage inMsg = rrObj.getRequest();
        ResolveType resolveType = validateIncomingMessage(inMsg, ResolveType.class);

        Optional.ofNullable(resolveType.getEndpointReference()).orElseThrow(() ->
                new SoapFaultException(soapFaultFactory.createSenderFault("Missing Endpoint Reference")));

        Optional.ofNullable(resolveType.getEndpointReference().getAddress()).orElseThrow(() ->
                new SoapFaultException(soapFaultFactory.createSenderFault("Missing Endpoint Reference Address")));

        EndpointReferenceType endpointReference = getEndpointReference();
        List<String> scopes = getScopes();
        List<QName> types = getTypes();
        List<String> xAddrs = getXAddrs();
        UnsignedInteger metadataVersion = getMetadataVersion();

        if (!URI.create(resolveType.getEndpointReference().getAddress().getValue())
                .equals(URI.create(endpointReference.getAddress().getValue()))) {
            throw new RuntimeException("Scopes and Types do not match in incoming Resolve.");
        }

        ResolveMatchType resolveMatchType = wsdFactory.createResolveMatchType();
        resolveMatchType.setEndpointReference(endpointReference);
        ScopesType scopesType = wsdFactory.createScopesType();
        scopesType.setValue(scopes);
        resolveMatchType.setScopes(scopesType);
        resolveMatchType.setTypes(types);
        resolveMatchType.setXAddrs(xAddrs);
        resolveMatchType.setMetadataVersion(metadataVersion.longValue());

        ResolveMatchesType resolveMatchesType = wsdFactory.createResolveMatchesType();
        resolveMatchesType.setResolveMatch(resolveMatchType);

        SoapMessage resolveMatchesMsg = rrObj.getResponse();
        WsAddressingHeader wsaHeader = resolveMatchesMsg.getWsAddressingHeader();
        wsaHeader.setAction(wsaUtil.createAttributedURIType(WsDiscoveryConstants.WSA_ACTION_RESOLVE_MATCHES));
        wsaHeader.setTo(wsaUtil.createAttributedURIType(WsDiscoveryConstants.WSA_UDP_TO));
        resolveMatchesMsg.getWsDiscoveryHeader().setAppSequence(wsdUtil.createAppSequence(instanceId));

        soapUtil.setBody(wsdFactory.createResolveMatches(resolveMatchesType), resolveMatchesMsg);
    }

    public EndpointReferenceType getEndpointReference() {
        return targetServiceEpr;
    }

    @Override
    public void setTypes(List<QName> qNames) {
        try {
            lock.lock();
            types = objectUtil.deepCopy(qNames);
            metadataModified.set(true);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<QName> getTypes() {
        try {
            lock.lock();
            return objectUtil.deepCopy(types);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void setScopes(List<String> uris) {
        try {
            lock.lock();
            scopes = objectUtil.deepCopy(uris);
            metadataModified.set(true);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<String> getScopes() {
        try {
            lock.lock();
            return objectUtil.deepCopy(scopes);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void setXAddrs(List<String> xAddrs) {
        try {
            lock.lock();
            this.xAddrs = objectUtil.deepCopy(xAddrs);
            metadataModified.set(true);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<String> getXAddrs() {
        try {
            lock.lock();
            return objectUtil.deepCopy(xAddrs);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void setMatchBy(MatchBy matchBy) {
        try {
            lock.lock();
            this.matchBy = matchBy;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public MatchBy getMatchBy() {
        try {
            lock.lock();
            return matchBy;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void setMetadataModified() {
        metadataModified.set(true);
    }

    @Override
    public UnsignedInteger getMetadataVersion() {
        try {
            lock.lock();
            return UnsignedInteger.valueOf(metadataVersion.longValue());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public UnsignedInteger sendHello() throws MarshallingException, TransportException, InterceptorException {
        return sendHello(false);
    }

    @Override
    public UnsignedInteger sendHello(boolean forceNewMetadataVersion) throws MarshallingException, TransportException, InterceptorException {
        UnsignedInteger currentMetadataVersion;
        if (forceNewMetadataVersion) {
            currentMetadataVersion = incMetadataVersionAndGet();
        } else {
            currentMetadataVersion = incMetadataVersionIfModifiedAndGet();
        }

        HelloType helloType = wsdFactory.createHelloType();
        helloType.setXAddrs(getXAddrs());
        ScopesType scopesType = wsdFactory.createScopesType();
        scopesType.setMatchBy(getMatchBy().getUri());
        scopesType.setValue(getScopes());
        helloType.setScopes(scopesType);
        helloType.setTypes(getTypes());
        helloType.setMetadataVersion(currentMetadataVersion.longValue());
        helloType.setEndpointReference(getEndpointReference());

        sendMulticast(WsDiscoveryConstants.WSA_ACTION_HELLO, wsdFactory.createHello(helloType));
        metadataModified.set(false);

        return currentMetadataVersion;
    }

    @Override
    public void sendBye() throws MarshallingException, TransportException, InterceptorException {
        ByeType byeType = wsdFactory.createByeType();
        byeType.setXAddrs(getXAddrs());
        ScopesType scopesType = wsdFactory.createScopesType();
        scopesType.setMatchBy(getMatchBy().getUri());
        scopesType.setValue(getScopes());
        byeType.setScopes(scopesType);
        byeType.setTypes(getTypes());
        byeType.setEndpointReference(getEndpointReference());

        sendMulticast(WsDiscoveryConstants.WSA_ACTION_BYE, wsdFactory.createBye(byeType));
    }

    public UnsignedInteger incMetadataVersionAndGet() {
        try {
            lock.lock();
            metadataVersion = getNewMetadataVersion(metadataVersion);
            return UnsignedInteger.valueOf(metadataVersion.longValue());
        } finally {
            lock.unlock();
        }
    }

    public UnsignedInteger incMetadataVersionIfModifiedAndGet() {
        try {
            lock.lock();
            if (metadataModified.get()) {
                metadataVersion = getNewMetadataVersion(metadataVersion);
            }
            return UnsignedInteger.valueOf(metadataVersion.longValue());
        } finally {
            lock.unlock();
        }
    }

    private UnsignedInteger getNewMetadataVersion(@Nullable UnsignedInteger currentVersion) {
        if (currentVersion == null) {
            return UnsignedInteger.valueOf((System.currentTimeMillis() / 1000L));
        }

        return currentVersion.plus(UnsignedInteger.ONE);
    }

    private void sendMulticast(String action, JAXBElement<?> body) throws MarshallingException, TransportException, InterceptorException {
        SoapMessage soapMessage = soapUtil.createMessage(action, WsDiscoveryConstants.WSA_UDP_TO, body);
        soapMessage.getWsDiscoveryHeader().setAppSequence(wsdUtil.createAppSequence(instanceId));
        notificationSource.sendNotification(soapMessage);
    }

    private <T> T validateIncomingMessage(SoapMessage request, Class<T> bodyType) throws SoapFaultException {
        String to = request.getWsAddressingHeader().getTo().orElseThrow(() ->
                new SoapFaultException(soapFaultFactory.createSenderFault(
                        String.format("wsa:To field is invalid. Expected '%s', but none found ",
                                WsDiscoveryConstants.WSA_UDP_TO)))).getValue();

        if (!to.equals(WsDiscoveryConstants.WSA_UDP_TO)) {
            throw new SoapFaultException(soapFaultFactory.createSenderFault(
                    String.format("wsa:To field is invalid. Expected '%s', but actual is '%s'",
                            WsDiscoveryConstants.WSA_UDP_TO, to)));
        }

        return soapUtil.getBody(request, bodyType).orElseThrow(() ->
                new SoapFaultException(soapFaultFactory.createSenderFault("Body type is invalid")));
    }
}
