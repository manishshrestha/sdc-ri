package it.org.ieee11073.sdc.dpws.soap;

import it.org.ieee11073.sdc.dpws.IntegrationTestUtil;
import org.apache.log4j.BasicConfigurator;
import org.ieee11073.sdc.dpws.client.ClientConfig;
import org.ieee11073.sdc.dpws.client.DiscoveredDevice;
import org.ieee11073.sdc.dpws.crypto.CryptoSettings;
import org.ieee11073.sdc.dpws.device.DeviceConfig;
import org.ieee11073.sdc.dpws.device.DeviceSettings;
import org.ieee11073.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.ieee11073.sdc.dpws.soap.SoapUtil;
import org.ieee11073.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.WsDiscoveryConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class CryptoIT {
    private static final Duration MAX_WAIT_TIME = Duration.ofMinutes(3);

    private final IntegrationTestUtil IT = new IntegrationTestUtil();
    private final SoapUtil soapUtil = IT.getInjector().getInstance(SoapUtil.class);
    private final WsAddressingUtil wsaUtil = IT.getInjector().getInstance(WsAddressingUtil.class);

    private DevicePeer devicePeer;
    private ClientPeer clientPeer;

    @Before
    public void setUp() throws Exception {
        BasicConfigurator.configure();
        final CryptoSettings serverCryptoSettings = Ssl.setupServer();

        this.devicePeer = new BasicPopulatedDevice(new DeviceSettings() {
            final EndpointReferenceType epr = wsaUtil.createEprWithAddress(soapUtil.createUriFromUuid(UUID.randomUUID()));

            @Override
            public EndpointReferenceType getEndpointReference() {
                return epr;
            }

            @Override
            public List<URI> getHostingServiceBindings() {
                return Collections.singletonList(URI.create("https://localhost:6464"));
            }
        }, new DefaultDpwsConfigModule() {
            @Override
            public void customConfigure() {
                bind(DeviceConfig.CRYPTO_SETTINGS, CryptoSettings.class, serverCryptoSettings);
            }
        });

        final CryptoSettings clientCryptoSettings = Ssl.setupClient();
        try {
            this.clientPeer = new ClientPeer(new DefaultDpwsConfigModule() {
                @Override
                public void customConfigure() {
                    bind(WsDiscoveryConfig.MAX_WAIT_FOR_PROBE_MATCHES, Duration.class,
                            Duration.ofSeconds(MAX_WAIT_TIME.getSeconds() / 2));
                    bind(ClientConfig.CRYPTO_SETTINGS, CryptoSettings.class, clientCryptoSettings);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @After
    public void tearDown() {
        this.devicePeer.stopAsync().awaitTerminated();
        this.clientPeer.stopAsync().awaitTerminated();
    }

    @Test
    public void directedProbeSecured() throws Exception {
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
}
