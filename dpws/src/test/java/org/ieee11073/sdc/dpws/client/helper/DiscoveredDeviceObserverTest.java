package org.ieee11073.sdc.dpws.client.helper;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.ieee11073.sdc.dpws.DpwsTest;
import org.ieee11073.sdc.dpws.client.*;
import org.ieee11073.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.ByeMessage;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.HelloMessage;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.ProbeMatchesMessage;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.ProbeTimeoutMessage;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.model.*;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.Assert.*;
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
    @Before
    public void setUp() throws Exception {
        super.setUp();
        WsAddressingUtil wsaUtil = getInjector().getInstance(WsAddressingUtil.class);
        ListeningExecutorService execService = MoreExecutors.newDirectExecutorService();
        discoveredDeviceResolver = mock(DiscoveredDeviceResolver.class);
        expectedUri = URI.create("http://expectedUri");
        expectedEpr = wsaUtil.createEprWithAddress(expectedUri);
        helloByeAndProbeMatchesObserverImpl = new HelloByeAndProbeMatchesObserverImpl(discoveredDeviceResolver, execService, wsaUtil);
        objFactory = new ObjectFactory();
        callbackVisitCount = 0;
    }

    @Test
    public void publishDeviceLeft() throws Exception {
        helloByeAndProbeMatchesObserverImpl.registerDiscoveryObserver(new org.ieee11073.sdc.dpws.client.DiscoveryObserver() {
            @Subscribe
            void onDeviceLeft(DeviceLeftMessage deviceLeftMessage) {
                assertEquals(expectedUri, deviceLeftMessage.getPayload());
                callbackVisitCount++;
            }
        });

        helloByeAndProbeMatchesObserverImpl.publishDeviceLeft(expectedUri, DeviceLeftMessage.TriggerType.BYE);
        assertEquals(1, callbackVisitCount);
    }

    @Test
    public void onHello() throws Exception {
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

        helloByeAndProbeMatchesObserverImpl.registerDiscoveryObserver(new org.ieee11073.sdc.dpws.client.DiscoveryObserver() {
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
    public void onBye() throws Exception {
        ByeType bType = objFactory.createByeType();
        bType.setEndpointReference(expectedEpr);
        ByeMessage bMsg = new ByeMessage(bType);

        helloByeAndProbeMatchesObserverImpl.registerDiscoveryObserver(new org.ieee11073.sdc.dpws.client.DiscoveryObserver() {
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
    public void onProbeMatches() throws Exception {
        ProbeMatchType pmType = objFactory.createProbeMatchType();
        pmType.setEndpointReference(expectedEpr);
        ProbeMatchesType pmsType = objFactory.createProbeMatchesType();
        pmsType.setProbeMatch(Arrays.asList(pmType));

        String expectedId = "expectedId";
        ProbeMatchesMessage pmMsg = new ProbeMatchesMessage(expectedId, pmsType);

        when(discoveredDeviceResolver.resolve(pmMsg))
                .thenReturn(Optional.of(new DiscoveredDevice(
                        expectedUri,
                        new ArrayList<>(),
                        new ArrayList<>(),
                        new ArrayList<>(),
                        1)));

        helloByeAndProbeMatchesObserverImpl.registerDiscoveryObserver(new org.ieee11073.sdc.dpws.client.DiscoveryObserver() {
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
    public void onProbeTimeout() throws Exception {
        Integer expectedDevicesCount = 10;
        String expectedId = "expectedId";
        ProbeTimeoutMessage ptMsg = new ProbeTimeoutMessage(expectedDevicesCount, expectedId);

        helloByeAndProbeMatchesObserverImpl.registerDiscoveryObserver(new org.ieee11073.sdc.dpws.client.DiscoveryObserver() {
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