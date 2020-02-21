package org.somda.sdc.dpws.client.helper;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.somda.sdc.dpws.DpwsTest;
import org.somda.sdc.dpws.client.DiscoveredDevice;
import org.somda.sdc.dpws.client.event.DeviceEnteredMessage;
import org.somda.sdc.dpws.client.event.DeviceLeftMessage;
import org.somda.sdc.dpws.client.event.DeviceProbeTimeoutMessage;
import org.somda.sdc.dpws.client.event.ProbedDeviceFoundMessage;
import org.somda.sdc.dpws.helper.ExecutorWrapperService;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.dpws.soap.wsdiscovery.event.ByeMessage;
import org.somda.sdc.dpws.soap.wsdiscovery.event.HelloMessage;
import org.somda.sdc.dpws.soap.wsdiscovery.event.ProbeMatchesMessage;
import org.somda.sdc.dpws.soap.wsdiscovery.event.ProbeTimeoutMessage;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ByeType;
import org.somda.sdc.dpws.soap.wsdiscovery.model.HelloType;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ObjectFactory;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ProbeMatchType;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ProbeMatchesType;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DiscoveredDeviceObserverTest extends DpwsTest {

    private DiscoveredDeviceResolver discoveredDeviceResolver;
    private URI expectedUri;
    private EndpointReferenceType expectedEpr;
    private HelloByeAndProbeMatchesObserverImpl helloByeAndProbeMatchesObserverImpl;
    private ObjectFactory objFactory;
    private int callbackVisitCount;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        WsAddressingUtil wsaUtil = getInjector().getInstance(WsAddressingUtil.class);
        ExecutorWrapperService<ListeningExecutorService> execService = new ExecutorWrapperService<>(
                MoreExecutors::newDirectExecutorService, "execService"
        );
        execService.startAsync().awaitRunning();
        discoveredDeviceResolver = mock(DiscoveredDeviceResolver.class);
        expectedUri = URI.create("http://expectedUri");
        expectedEpr = wsaUtil.createEprWithAddress(expectedUri);
        helloByeAndProbeMatchesObserverImpl = new HelloByeAndProbeMatchesObserverImpl(discoveredDeviceResolver, execService, wsaUtil);
        objFactory = new ObjectFactory();
        callbackVisitCount = 0;
    }

    @Test
    public void publishDeviceLeft() {
        helloByeAndProbeMatchesObserverImpl.registerDiscoveryObserver(new org.somda.sdc.dpws.client.DiscoveryObserver() {
            @Subscribe
            void onDeviceLeft(DeviceLeftMessage deviceLeftMessage) {
                assertEquals(expectedUri, deviceLeftMessage.getPayload());
                callbackVisitCount++;
            }
        });

        helloByeAndProbeMatchesObserverImpl.publishDeviceLeft(expectedUri, DeviceLeftMessage.TriggeredBy.BYE);
        assertEquals(1, callbackVisitCount);
    }

    @Test
    public void onHello() {
        HelloType hType = objFactory.createHelloType();
        hType.setEndpointReference(expectedEpr);
        HelloMessage hMsg = new HelloMessage(hType);

        when(discoveredDeviceResolver.resolve(hMsg))
                .thenReturn(Optional.of(new DiscoveredDevice(
                        expectedUri,
                        new ArrayList<>(),
                        new ArrayList<>(),
                        new ArrayList<>(),
                        1)));

        helloByeAndProbeMatchesObserverImpl.registerDiscoveryObserver(new org.somda.sdc.dpws.client.DiscoveryObserver() {
            @Subscribe
            void onDeviceEntered(DeviceEnteredMessage deviceEntered) {
                assertEquals(expectedUri, deviceEntered.getPayload().getEprAddress());
                callbackVisitCount++;
            }
        });

        helloByeAndProbeMatchesObserverImpl.onHello(hMsg);
        assertEquals(1, callbackVisitCount);
    }

    @Test
    public void onBye() {
        ByeType bType = objFactory.createByeType();
        bType.setEndpointReference(expectedEpr);
        ByeMessage bMsg = new ByeMessage(bType);

        helloByeAndProbeMatchesObserverImpl.registerDiscoveryObserver(new org.somda.sdc.dpws.client.DiscoveryObserver() {
            @Subscribe
            void onDeviceLeft(DeviceLeftMessage deviceLeftMessage) {
                assertEquals(expectedUri, deviceLeftMessage.getPayload());
                callbackVisitCount++;
            }
        });

        helloByeAndProbeMatchesObserverImpl.onBye(bMsg);
        assertEquals(1, callbackVisitCount);
    }

    @Test
    public void onProbeMatches() {
        ProbeMatchType pmType = objFactory.createProbeMatchType();
        pmType.setEndpointReference(expectedEpr);
        ProbeMatchesType pmsType = objFactory.createProbeMatchesType();
        pmsType.setProbeMatch(Collections.singletonList(pmType));

        String expectedId = "expectedId";
        ProbeMatchesMessage pmMsg = new ProbeMatchesMessage(expectedId, pmsType);

        when(discoveredDeviceResolver.resolve(pmMsg))
                .thenReturn(Optional.of(new DiscoveredDevice(
                        expectedUri,
                        new ArrayList<>(),
                        new ArrayList<>(),
                        new ArrayList<>(),
                        1)));

        helloByeAndProbeMatchesObserverImpl.registerDiscoveryObserver(new org.somda.sdc.dpws.client.DiscoveryObserver() {
            @Subscribe
            void onProbedDevice(ProbedDeviceFoundMessage probedDeviceFound) {
                assertEquals(expectedId, probedDeviceFound.getDiscoveryId());
                assertEquals(expectedUri, probedDeviceFound.getPayload().getEprAddress());
                callbackVisitCount++;
            }
        });

        helloByeAndProbeMatchesObserverImpl.onProbeMatches(pmMsg);
        assertEquals(1, callbackVisitCount);
    }

    @Test
    public void onProbeTimeout() {
        Integer expectedDevicesCount = 10;
        String expectedId = "expectedId";
        ProbeTimeoutMessage ptMsg = new ProbeTimeoutMessage(expectedDevicesCount, expectedId);

        helloByeAndProbeMatchesObserverImpl.registerDiscoveryObserver(new org.somda.sdc.dpws.client.DiscoveryObserver() {
            @Subscribe
            void onTimeout(DeviceProbeTimeoutMessage deviceProbeTimeout) {
                assertEquals(expectedDevicesCount, deviceProbeTimeout.getFoundDevicesCount());
                assertEquals(expectedId, deviceProbeTimeout.getDiscoveryId());
                callbackVisitCount++;
            }
        });

        helloByeAndProbeMatchesObserverImpl.onProbeTimeout(ptMsg);
        assertEquals(1, callbackVisitCount);
    }
}