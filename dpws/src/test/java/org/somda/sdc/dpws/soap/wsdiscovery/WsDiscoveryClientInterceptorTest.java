package org.somda.sdc.dpws.soap.wsdiscovery;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.somda.sdc.common.util.ExecutorWrapperService;
import org.somda.sdc.dpws.DpwsTest;
import org.somda.sdc.dpws.guice.WsDiscovery;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.NotificationSink;
import org.somda.sdc.dpws.soap.NotificationSource;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.TransportInfo;
import org.somda.sdc.dpws.soap.factory.EnvelopeFactory;
import org.somda.sdc.dpws.soap.factory.NotificationSinkFactory;
import org.somda.sdc.dpws.soap.factory.NotificationSourceFactory;
import org.somda.sdc.dpws.soap.factory.SoapMessageFactory;
import org.somda.sdc.dpws.soap.model.Envelope;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingClientInterceptor;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingServerInterceptor;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.AttributedURIType;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.dpws.soap.wsaddressing.model.ObjectFactory;
import org.somda.sdc.dpws.soap.wsdiscovery.event.ProbeMatchesMessage;
import org.somda.sdc.dpws.soap.wsdiscovery.factory.WsDiscoveryClientFactory;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ProbeMatchType;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ProbeMatchesType;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ResolveMatchType;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ResolveMatchesType;

import javax.annotation.Nullable;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WsDiscoveryClientInterceptorTest extends DpwsTest {
    private List<SoapMessage> sentSoapMessages;
    private WsDiscoveryClient wsDiscoveryClient;

    private List<QName> expectedTypes;
    private List<String> expectedScopes;
    private NotificationSource notificationSource;
    private EndpointReferenceType expectedEpr;
    private org.somda.sdc.dpws.soap.wsdiscovery.model.ObjectFactory wsdFactory;
    private SoapMessageFactory soapMessageFactory;
    private EnvelopeFactory envelopeFactory;
    private NotificationSink notificationSink;
    private CommunicationContext communicationContextMock;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // start required thread pool(s)
        getInjector().getInstance(Key.get(
                new TypeLiteral<ExecutorWrapperService<ListeningExecutorService>>() {
                },
                WsDiscovery.class
        )).startAsync().awaitRunning();

        communicationContextMock = mock(CommunicationContext.class);
        var transportInfoMock = mock(TransportInfo.class);
        when(communicationContextMock.getTransportInfo()).thenReturn(transportInfoMock);
        when(transportInfoMock.getScheme()).thenReturn("any");

        soapMessageFactory = getInjector().getInstance(SoapMessageFactory.class);
        NotificationSourceFactory nSourceFactory = getInjector().getInstance(NotificationSourceFactory.class);
        sentSoapMessages = new ArrayList<>();
        notificationSource = nSourceFactory.createNotificationSource(notification -> sentSoapMessages.add(notification));

        notificationSink = getInjector().getInstance(NotificationSinkFactory.class).createNotificationSink(
                getInjector().getInstance(WsAddressingServerInterceptor.class));

        WsAddressingUtil wsaUtil = getInjector().getInstance(WsAddressingUtil.class);

        WsDiscoveryClientFactory wsdClientFactory = getInjector().getInstance(WsDiscoveryClientFactory.class);
        wsDiscoveryClient = wsdClientFactory.createWsDiscoveryClient(notificationSource);
        //notificationSink.registerOrUpdate(wsDiscoveryClient);

        QName expectedType = new QName("http://namespace", "type");
        String expectedScope = "http://namespace/scope";
        String expectedXAddr = "http://1.2.3.4";

        expectedTypes = new ArrayList<>();
        expectedTypes.add(expectedType);

        expectedScopes = new ArrayList<>();
        expectedScopes.add(expectedScope);

        ObjectFactory wsaFactory = getInjector().getInstance(ObjectFactory.class);
        expectedEpr = wsaFactory.createEndpointReferenceType();
        AttributedURIType eprUri = wsaFactory.createAttributedURIType();
        eprUri.setValue("http://expectedEpr-uri");
        expectedEpr.setAddress(eprUri);

        wsdFactory = getInjector().getInstance(org.somda.sdc.dpws.soap.wsdiscovery.model.ObjectFactory.class);
        envelopeFactory = getInjector().getInstance(EnvelopeFactory.class);
    }

    @Test
    public void processProbe() {
        processProbeOrResolveRequestWithCallback(() -> {
            try {
                wsDiscoveryClient.sendProbe(UUID.randomUUID().toString(), expectedTypes, expectedScopes);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void processResolve() {
        processProbeOrResolveRequestWithCallback(() -> {
            try {
                wsDiscoveryClient.sendResolve(expectedEpr);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void processProbeOrResolveRequestWithCallback(Runnable r) {
        r.run();
        assertEquals(1, sentSoapMessages.size());
        assertFalse(sentSoapMessages.get(0).getWsDiscoveryHeader().getAppSequence().isPresent());

        notificationSource.register(wsDiscoveryClient);

        r.run();
        assertEquals(2, sentSoapMessages.size());
        assertTrue(sentSoapMessages.get(1).getWsDiscoveryHeader().getAppSequence().isPresent());
    }

    @Test
    public void sendProbe() throws Exception {
        notificationSource.register(getInjector().getInstance(WsAddressingClientInterceptor.class));
        notificationSource.register(wsDiscoveryClient);
        notificationSink.register(wsDiscoveryClient);

        final String searchId = UUID.randomUUID().toString();

        // TODO: 26.01.2017 Add reception of probe answers
        wsDiscoveryClient.registerHelloByeAndProbeMatchesObserver(new HelloByeAndProbeMatchesObserver() {
            @Subscribe
            void onProbe(ProbeMatchesMessage probeMatchesMessage) {
                assertEquals(searchId, probeMatchesMessage.getProbeRequestId());
            }
        });

        ListenableFuture<Integer> future = wsDiscoveryClient.sendProbe(searchId, expectedTypes,
                expectedScopes, 1);
        assertEquals(1, sentSoapMessages.size());

        notificationSink.receiveNotification(createProbeMatches(sentSoapMessages.get(0)), communicationContextMock);

        assertEquals(Integer.valueOf(1), future.get());
    }

    @Test
    public void sendResolve() throws Exception {
        notificationSource.register(getInjector().getInstance(WsAddressingClientInterceptor.class));
        notificationSource.register(wsDiscoveryClient);
        notificationSink.register(wsDiscoveryClient);

        ListenableFuture<ResolveMatchesType> result = wsDiscoveryClient.sendResolve(expectedEpr);
        assertEquals(1, sentSoapMessages.size());
        notificationSink.receiveNotification(createResolveMatches(sentSoapMessages.get(0)), communicationContextMock);

        Futures.addCallback(result, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable ResolveMatchesType resolveMatchesType) {
                assertTrue(true);
            }

            @Override
            public void onFailure(Throwable throwable) {
                fail(throwable::getMessage);
            }
        }, MoreExecutors.directExecutor());
    }

    private SoapMessage createResolveMatches(SoapMessage msg) {
        ResolveMatchType resolveMatchType = wsdFactory.createResolveMatchType();
        ResolveMatchesType resolveMatchesType = wsdFactory.createResolveMatchesType();
        resolveMatchesType.setResolveMatch(resolveMatchType);

        Envelope env = envelopeFactory.createEnvelope(WsDiscoveryConstants.WSA_ACTION_RESOLVE_MATCHES,
                WsDiscoveryConstants.WSA_UDP_TO, wsdFactory.createResolveMatches(resolveMatchesType));
        SoapMessage rMatches = soapMessageFactory.createSoapMessage(env);

        rMatches.getWsAddressingHeader().setRelatesTo(msg.getWsAddressingHeader().getMessageId().orElse(null));
        return rMatches;
    }

    private SoapMessage createProbeMatches(SoapMessage msg) {
        ProbeMatchType probeMatchType = wsdFactory.createProbeMatchType();
        ProbeMatchesType probeMatchesType = wsdFactory.createProbeMatchesType();
        List<ProbeMatchType> probeMatchTypes = new ArrayList<>();
        probeMatchTypes.add(probeMatchType);
        probeMatchesType.setProbeMatch(probeMatchTypes);

        Envelope env = envelopeFactory.createEnvelope(WsDiscoveryConstants.WSA_ACTION_PROBE_MATCHES,
                WsDiscoveryConstants.WSA_UDP_TO, wsdFactory.createProbeMatches(probeMatchesType));
        SoapMessage pMatches = soapMessageFactory.createSoapMessage(env);

        pMatches.getWsAddressingHeader().setRelatesTo(msg.getWsAddressingHeader().getMessageId().orElse(null));
        var msgId = getInjector().getInstance(SoapUtil.class).createRandomUuidUri();
        AttributedURIType uriType = getInjector().getInstance(WsAddressingUtil.class).createAttributedURIType(msgId);
        pMatches.getWsAddressingHeader().setMessageId(uriType);

        return pMatches;
    }
}