package it.org.somda.sdc.proto.discovery;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Injector;
import it.org.somda.sdc.proto.IntegrationTestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.dpws.DpwsFramework;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.proto.discovery.consumer.Client;
import org.somda.sdc.proto.discovery.consumer.event.DeviceEnteredMessage;
import org.somda.sdc.proto.discovery.consumer.event.ProbedDeviceFoundMessage;
import org.somda.sdc.proto.discovery.provider.TargetService;
import org.somda.sdc.proto.discovery.provider.factory.TargetServiceFactory;
import org.somda.sdc.proto.model.discovery.DiscoveryTypes;
import org.somda.sdc.proto.model.discovery.Endpoint;
import org.somda.sdc.proto.model.discovery.ScopeMatcher;
import test.org.somda.common.LoggingTestWatcher;
import test.org.somda.common.TimedWait;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(LoggingTestWatcher.class)
public class DiscoveryIT {
    private Injector injector;
    private String epr;
    private TargetService targetService;
    private Client client;
    private DiscoveryObserver observer;
    private DpwsFramework dpwsFramework;

    @BeforeEach
    void beforeEach() throws Exception {
        this.injector = new IntegrationTestUtil().getInjector();
        this.epr = injector.getInstance(SoapUtil.class).createRandomUuidUri();
        this.targetService = injector.getInstance(TargetServiceFactory.class).create(epr);
        this.client = injector.getInstance(Client.class);
        this.observer = new DiscoveryObserver();
        this.dpwsFramework = injector.getInstance(DpwsFramework.class);
        dpwsFramework.setNetworkInterface(NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress()));
        dpwsFramework.startAsync().awaitRunning();
    }

    @AfterEach
    void afterEach() {
        client.stopAsync().awaitTerminated();
        targetService.stopAsync().awaitTerminated();
        dpwsFramework.stopAsync().awaitTerminated();
    }

    @Test
    @DisplayName("Hello")
    void testHello() throws Exception {
        client.registerObserver(observer);
        client.startAsync().awaitRunning();
        targetService.startAsync().awaitRunning();
        assertTrue(observer.waitForMessages(1, Duration.ofSeconds(10)));
        assertEquals(epr, observer.timedWait.getData().get(0).getEndpointReference().getAddress());
    }

    @Test
    @DisplayName("Probe")
    void testProbe() throws Exception {
        var scope = "http://scope";
        var xAddr = "http://127.0.0.1";
        targetService.updateScopes(Collections.singleton(scope));
        targetService.updateXAddrs(Collections.singleton(xAddr));
        targetService.startAsync().awaitRunning();
        client.registerObserver(observer);
        client.startAsync().awaitRunning();
        var probe = client.probe(ScopeMatcher.newBuilder().addScopes(scope).build(), 1);
        var endpoints = probe.get(5, TimeUnit.SECONDS);
        assertEquals(1, endpoints.size());
        assertEquals(1, endpoints.get(0).getScopeList().size());
        assertEquals(scope, endpoints.get(0).getScope(0));
        assertEquals(1, endpoints.get(0).getXAddrList().size());
        assertEquals(xAddr, endpoints.get(0).getXAddr(0));

        assertTrue(observer.waitForMessages(2, Duration.ofSeconds(5)));
        assertEquals(2, observer.timedWait.getData().size());
    }

    @Test
    @DisplayName("Resolve")
    void testResolve() throws Exception {
        var scope = "http://scope";
        var xAddr = "http://127.0.0.1";
        targetService.updateScopes(Collections.singleton(scope));
        targetService.updateXAddrs(Collections.singleton(xAddr));
        targetService.startAsync().awaitRunning();
        client.registerObserver(observer);
        client.startAsync().awaitRunning();
        var resolve = client.resolve(epr);
        var endpoint = resolve.get(5, TimeUnit.SECONDS);
        assertEquals(epr, endpoint.getEndpointReference().getAddress());
        assertEquals(1, endpoint.getScopeList().size());
        assertEquals(scope, endpoint.getScope(0));
        assertEquals(1, endpoint.getXAddrList().size());
        assertEquals(xAddr, endpoint.getXAddr(0));
    }

    private class DiscoveryObserver implements org.somda.sdc.proto.discovery.consumer.DiscoveryObserver {
        TimedWait<List<Endpoint>> timedWait = new TimedWait<>(ArrayList::new);

        @Subscribe
        void onEnteredDevice(DeviceEnteredMessage message) {
            timedWait.modifyData(testNotifications -> testNotifications.add(message.getPayload()));
        }

        @Subscribe
        void onEnteredDevice(ProbedDeviceFoundMessage message) {
            timedWait.modifyData(testNotifications -> testNotifications.addAll(message.getPayload()));
        }

        boolean waitForMessages(int messageCount, Duration wait) {
            return timedWait.waitForData(notifications -> notifications.size() >= messageCount, wait);
        }
    }
}
