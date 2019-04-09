package org.ieee11073.sdc.dpws.client.helper;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.ieee11073.sdc.dpws.DpwsTest;
import org.ieee11073.sdc.dpws.client.*;
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

public class DeviceProxyObserverTest extends DpwsTest {

    private DeviceProxyResolver deviceProxyResolver;
    private URI expectedUri;
    private EndpointReferenceType expectedEpr;
    private DeviceProxyObserver deviceProxyObserver;
    private ObjectFactory objFactory;
    private int callbackVisitCount;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        WsAddressingUtil wsaUtil = getInjector().getInstance(WsAddressingUtil.class);
        ListeningExecutorService execService = MoreExecutors.newDirectExecutorService();
        deviceProxyResolver = mock(DeviceProxyResolver.class);
        expectedUri = URI.create("http://expectedUri");
        expectedEpr = wsaUtil.createEprWithAddress(expectedUri);
        deviceProxyObserver = new DeviceProxyObserver(deviceProxyResolver, execService, wsaUtil);
        objFactory = new ObjectFactory();
        callbackVisitCount = 0;
    }

    @Test
    public void publishDeviceLeft() throws Exception {
        deviceProxyObserver.registerDiscoveryObserver(new DiscoveryObserver() {
            @Subscribe
            void onDeviceLeft(DeviceLeftMessage deviceLeftMessage) {
                assertEquals(expectedUri, deviceLeftMessage.getPayload());
                callbackVisitCount++;
            }
        });

        deviceProxyObserver.publishDeviceLeft(expectedUri, DeviceLeftMessage.TriggerType.BYE);
        assertEquals(1, callbackVisitCount);
    }

    @Test
    public void onHello() throws Exception {
        HelloType hType = objFactory.createHelloType();
        hType.setEndpointReference(expectedEpr);
        HelloMessage hMsg = new HelloMessage(hType);

        when(deviceProxyResolver.resolve(hMsg))
                .thenReturn(Optional.of(new DeviceProxy(
                        expectedUri,
                        new ArrayList<>(),
                        new ArrayList<>(),
                        new ArrayList<>(),
                        1)));

        deviceProxyObserver.registerDiscoveryObserver(new DiscoveryObserver() {
            @Subscribe
            void onDeviceEntered(DeviceEnteredMessage deviceEntered) {
                assertEquals(expectedUri, deviceEntered.getPayload().getEprAddress());
                callbackVisitCount++;
            }
        });

        deviceProxyObserver.onHello(hMsg);
        assertEquals(1, callbackVisitCount);
    }

    @Test
    public void onBye() throws Exception {
        ByeType bType = objFactory.createByeType();
        bType.setEndpointReference(expectedEpr);
        ByeMessage bMsg = new ByeMessage(bType);

        deviceProxyObserver.registerDiscoveryObserver(new DiscoveryObserver() {
            @Subscribe
            void onDeviceLeft(DeviceLeftMessage deviceLeftMessage) {
                assertEquals(expectedUri, deviceLeftMessage.getPayload());
                callbackVisitCount++;
            }
        });

        deviceProxyObserver.onBye(bMsg);
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

        when(deviceProxyResolver.resolve(pmMsg))
                .thenReturn(Optional.of(new DeviceProxy(
                        expectedUri,
                        new ArrayList<>(),
                        new ArrayList<>(),
                        new ArrayList<>(),
                        1)));

        deviceProxyObserver.registerDiscoveryObserver(new DiscoveryObserver() {
            @Subscribe
            void onProbedDevice(ProbedDeviceFoundMessage probedDeviceFound) {
                assertEquals(expectedId, probedDeviceFound.getDiscoveryId());
                assertEquals(expectedUri, probedDeviceFound.getPayload().getEprAddress());
                callbackVisitCount++;
            }
        });

        deviceProxyObserver.onProbeMatches(pmMsg);
        assertEquals(1, callbackVisitCount);
    }

    @Test
    public void onProbeTimeout() throws Exception {
        Integer expectedDevicesCount = 10;
        String expectedId = "expectedId";
        ProbeTimeoutMessage ptMsg = new ProbeTimeoutMessage(expectedDevicesCount, expectedId);

        deviceProxyObserver.registerDiscoveryObserver(new DiscoveryObserver() {
            @Subscribe
            void onTimeout(DeviceProbeTimeoutMessage deviceProbeTimeout) {
                assertEquals(expectedDevicesCount, deviceProbeTimeout.getFoundDevicesCount());
                assertEquals(expectedId, deviceProbeTimeout.getDiscoveryId());
                callbackVisitCount++;
            }
        });

        deviceProxyObserver.onProbeTimeout(ptMsg);
        assertEquals(1, callbackVisitCount);
    }
}