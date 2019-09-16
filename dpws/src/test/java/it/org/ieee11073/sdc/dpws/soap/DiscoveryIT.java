package it.org.ieee11073.sdc.dpws.soap;


import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import it.org.ieee11073.sdc.dpws.IntegrationTestUtil;
import it.org.ieee11073.sdc.dpws.MockedUdpBindingModule;
import it.org.ieee11073.sdc.dpws.TestServiceMetadata;
import org.ieee11073.sdc.dpws.client.*;
import org.ieee11073.sdc.dpws.client.event.DeviceEnteredMessage;
import org.ieee11073.sdc.dpws.client.event.DeviceProbeTimeoutMessage;
import org.ieee11073.sdc.dpws.client.event.ProbedDeviceFoundMessage;
import org.ieee11073.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.ieee11073.sdc.dpws.service.HostingServiceProxy;
import org.ieee11073.sdc.dpws.soap.SoapConfig;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.WsDiscoveryConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import test.org.ieee11073.common.TestLogging;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class DiscoveryIT {
    private static final Duration MAX_WAIT_TIME = IntegrationTestUtil.MAX_WAIT_TIME;

    private DevicePeer devicePeer;
    private ClientPeer clientPeer;

    public DiscoveryIT() {
        IntegrationTestUtil.preferIpV4Usage();
    }

    @Before
    public void setUp() {
        TestLogging.configure();
        this.devicePeer = new BasicPopulatedDevice(new MockedUdpBindingModule());
        this.clientPeer = new ClientPeer(new DefaultDpwsConfigModule() {
            @Override
            public void customConfigure() {
                bind(WsDiscoveryConfig.MAX_WAIT_FOR_PROBE_MATCHES, Duration.class,
                        Duration.ofSeconds(MAX_WAIT_TIME.getSeconds() / 2));
                bind(SoapConfig.JAXB_CONTEXT_PATH, String.class,
                        TestServiceMetadata.JAXB_CONTEXT_PATH);
            }
        }, new MockedUdpBindingModule());
    }

    @After
    public void tearDown() {
        this.devicePeer.stopAsync().awaitTerminated();
        this.clientPeer.stopAsync().awaitTerminated();
    }

    @Test
    public void explicitDeviceDiscovery() throws Exception {
        // Given a device under test (DUT) and a client up and running
        devicePeer.startAsync().awaitRunning();
        clientPeer.startAsync().awaitRunning();

        // Given a discovery observer
        final SettableFuture<Integer> actualDeviceFoundCount = SettableFuture.create();
        final SettableFuture<String> actualEpr = SettableFuture.create();
        DiscoveryObserver obs = new DiscoveryObserver() {
            @Subscribe
            void deviceFound(ProbedDeviceFoundMessage message) {
                if (devicePeer.getEprAddress().equals(message.getPayload().getEprAddress())) {
                    actualEpr.set(message.getPayload().getEprAddress().toString());
                }
            }

            @Subscribe
            void timeout(DeviceProbeTimeoutMessage message) {
            }
        };

        // When explicit discovery is triggered (filter set to none)
        clientPeer.getClient().registerDiscoveryObserver(obs);
        DiscoveryFilterBuilder discoveryFilterBuilder = new DiscoveryFilterBuilder();
        clientPeer.getClient().probe(discoveryFilterBuilder.get());

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
    public void connectWithDiscoveredDevice() throws Exception {
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

    @Test
    public void connectWithEprAddress() throws Exception {
        // Given a device under test (DUT) and a client up and running
        devicePeer.startAsync().awaitRunning();
        clientPeer.startAsync().awaitRunning();

        // When the client connects to the DUT
        final ListenableFuture<HostingServiceProxy> hostingServiceProxyFuture = clientPeer.getClient()
                .connect(devicePeer.getEprAddress());

        // Then expect a hosting service to be resolved that matches the DUT EPR address
        final String expectedEprAddress = devicePeer.getEprAddress().toString();
        final HostingServiceProxy hostingServiceProxy = hostingServiceProxyFuture.get(MAX_WAIT_TIME.getSeconds(),
                TimeUnit.SECONDS);
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
