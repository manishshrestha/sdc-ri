package it.org.ieee11073.sdc.dpws.soap;


import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.SettableFuture;
import org.ieee11073.sdc.dpws.DpwsTest;
import org.ieee11073.sdc.dpws.client.*;
import org.ieee11073.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.WsDiscoveryConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CommunicationIT extends DpwsTest {
    private static final Duration MAX_WAIT_TIME = Duration.ofSeconds(20);

    private DevicePeer devicePeer;
    private ClientPeer clientPeer;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.devicePeer = new DevicePeer();
        this.clientPeer = new ClientPeer(new DefaultDpwsConfigModule() {
            @Override
            protected void customConfigure() {
                // shorten the test time by only waiting 1 second for the probe response
                bind(WsDiscoveryConfig.MAX_WAIT_FOR_PROBE_MATCHES, Duration.class, Duration.ofSeconds((long)MAX_WAIT_TIME.getSeconds() / 2));
            }
        });
    }

    @After
    public void tearDown() throws Exception {
        this.devicePeer.stopAsync().awaitTerminated();
        this.clientPeer.stopAsync().awaitTerminated();
    }

    @Test
    public void explicitDeviceDiscovery() throws Exception {
        devicePeer.startAsync().awaitRunning();
        clientPeer.startAsync().awaitRunning();
        final SettableFuture<Integer> actualDeviceFoundCount = SettableFuture.create();
        final SettableFuture<String> actualEpr = SettableFuture.create();
        DiscoveryObserver obs = new DiscoveryObserver() {
            private String discoveryId = "";
            private int deviceFoundCount = 0;

            @Subscribe
            void deviceFound(ProbedDeviceFoundMessage message) {
                if (devicePeer.getEprAddress().equals(message.getPayload().getEprAddress())) {
                    deviceFoundCount++;
                    discoveryId = message.getDiscoveryId();
                    actualEpr.set(message.getPayload().getEprAddress().toString());
                }
            }

            @Subscribe
            void timeout(DeviceProbeTimeoutMessage message) {
                if (discoveryId != "" && message.getDiscoveryId() == discoveryId) {
                    assertEquals(deviceFoundCount, message.getFoundDevicesCount().intValue());
                    actualDeviceFoundCount.set(deviceFoundCount);
                }
            }
        };

        clientPeer.getClient().registerDiscoveryObserver(obs);
        DiscoveryFilterBuilder discoveryFilterBuilder = new DiscoveryFilterBuilder();
        clientPeer.getClient().probe(discoveryFilterBuilder.get());

        assertEquals(1, actualDeviceFoundCount.get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS).intValue());
        assertEquals(devicePeer.getEprAddress().toString(), actualEpr.get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS));

        assertEquals(devicePeer.getEprAddress().toString(),
                clientPeer.getClient().resolve(devicePeer.getEprAddress()).get(5, TimeUnit.SECONDS)
                        .getResolveMatch().getEndpointReference().getAddress().getValue());
    }

    @Test
    public void implicitDeviceDiscovery() throws Exception {
        clientPeer.startAsync().awaitRunning();
        final SettableFuture<String> actualEpr = SettableFuture.create();
        DiscoveryObserver obs = new DiscoveryObserver() {
            @Subscribe
            void deviceFound(DeviceEnteredMessage message) {
                if (devicePeer.getEprAddress().equals(message.getPayload().getEprAddress())) {
                    actualEpr.set(message.getPayload().getEprAddress().toString());
                }
            }
        };
        clientPeer.getClient().registerDiscoveryObserver(obs);
        devicePeer.startAsync().awaitRunning();

        assertEquals(devicePeer.getEprAddress().toString(), actualEpr.get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS));

        assertEquals(devicePeer.getEprAddress().toString(),
                clientPeer.getClient().resolve(devicePeer.getEprAddress()).get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS)
                        .getResolveMatch().getEndpointReference().getAddress().getValue());
    }
}
