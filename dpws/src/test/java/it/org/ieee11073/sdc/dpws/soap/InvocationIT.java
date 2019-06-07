package it.org.ieee11073.sdc.dpws.soap;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.apache.log4j.BasicConfigurator;
import org.ieee11073.sdc.dpws.client.*;
import org.ieee11073.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.ieee11073.sdc.dpws.service.HostingServiceProxy;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.WsDiscoveryConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class InvocationIT {
    private static final Duration MAX_WAIT_TIME = Duration.ofSeconds(10);

    private DevicePeer devicePeer;
    private ClientPeer clientPeer;

    @Before
    public void setUp() throws Exception {
        BasicConfigurator.configure();
        this.devicePeer = new BasicPopulatedDevice();
        this.clientPeer = new ClientPeer();
        devicePeer.startAsync().awaitRunning();
        clientPeer.startAsync().awaitRunning();
    }

    @After
    public void tearDown() throws Exception {
        this.devicePeer.stopAsync().awaitTerminated();
        this.clientPeer.stopAsync().awaitTerminated();
    }

    @Test
    public void requestResponse() throws Exception {
        // Given a device under test (DUT) and a client up and running
        devicePeer.startAsync().awaitRunning();
        clientPeer.startAsync().awaitRunning();

        // Given a discovery observer
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

        // When explicit discovery is triggered (filter set to none)
        clientPeer.getClient().registerDiscoveryObserver(obs);
        DiscoveryFilterBuilder discoveryFilterBuilder = new DiscoveryFilterBuilder();
        clientPeer.getClient().probe(discoveryFilterBuilder.get());

        // Then expect to find one device
        final int expectedDeviceFoundCount = 1;
        assertEquals(expectedDeviceFoundCount, actualDeviceFoundCount
                .get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS).intValue());

        // Then expect the found EPR address to be the DUT's EPR address
        final String expectedEprAddress = devicePeer.getEprAddress().toString();
        assertEquals(expectedEprAddress, actualEpr.get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS));

        // Resolve and verify EPR address against the DUT's EPR address
        assertEquals(expectedEprAddress,
                clientPeer.getClient().resolve(devicePeer.getEprAddress()).get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS)
                        .getEprAddress().toString());
    }

    @Test
    public void implicitDeviceDiscovery() throws Exception {
        // Given a running client that listens to discovered devices
        clientPeer.startAsync().awaitRunning();
        final SettableFuture<String> actualEpr = SettableFuture.create();
        final DiscoveryObserver obs = new DiscoveryObserver() {
            @Subscribe
            void deviceFound(DeviceEnteredMessage message) {
                if (devicePeer.getEprAddress().equals(message.getPayload().getEprAddress())) {
                    actualEpr.set(message.getPayload().getEprAddress().toString());
                }
            }
        };
        clientPeer.getClient().registerDiscoveryObserver(obs);

        // When a device under test (DUT) joins the network
        devicePeer.startAsync().awaitRunning();

        // Then expect the found EPR address to be the DUT's EPR address
        final String expectedEprAddress = devicePeer.getEprAddress().toString();
        assertEquals(expectedEprAddress, actualEpr.get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS));

        // Resolve and verify EPR address against the DUT's EPR address
        assertEquals(expectedEprAddress,
                clientPeer.getClient().resolve(devicePeer.getEprAddress()).get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS)
                        .getEprAddress().toString());
    }

    @Test
    public void directedProbe() throws Exception {
        // Given a device under test (DUT) and a client up and running
        devicePeer.startAsync().awaitRunning();
        clientPeer.startAsync().awaitRunning();

        // When the DUT's physical addresses are resolved
        final DiscoveredDevice discoveredDevice = clientPeer.getClient().resolve(devicePeer.getEprAddress())
                .get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        final List<String> xAddrs = discoveredDevice.getXAddrs();
        assertFalse(xAddrs.isEmpty());
        final URI uri = URI.create(xAddrs.get(0));

        // Then expect the EPR address returned by a directed probe to be the DUT's EPR address
        final String expectedEprAddress = devicePeer.getEprAddress().toString();

        assertEquals(expectedEprAddress, clientPeer.getClient().directedProbe(uri)
                .get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS)
                .getProbeMatch().get(0).getEndpointReference().getAddress().getValue());
    }

    @Test
    public void connect() throws Exception {
        // Given a device under test (DUT) and a client up and running
        devicePeer.startAsync().awaitRunning();
        clientPeer.startAsync().awaitRunning();

        // When the client connects to the DUT
        final DiscoveredDevice discoveredDevice = clientPeer.getClient().resolve(devicePeer.getEprAddress())
                .get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        final ListenableFuture<HostingServiceProxy> hostingServiceProxyFuture = clientPeer.getClient().connect(discoveredDevice);

        // Then expect a hosting service to be resolved that matches the DUT EPR address
        final String expectedEprAddress = devicePeer.getEprAddress().toString();
        final HostingServiceProxy hostingServiceProxy = hostingServiceProxyFuture.get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        final String actualEprAddress = hostingServiceProxy.getEndpointReferenceAddress().toString();
        assertEquals(expectedEprAddress, actualEprAddress);
    }

    // @Test
    public void sample() throws Exception {
        // Given a device under test (DUT) and a client up and running
        devicePeer.startAsync().awaitRunning();
        Thread.sleep(100000000);
    }
}
