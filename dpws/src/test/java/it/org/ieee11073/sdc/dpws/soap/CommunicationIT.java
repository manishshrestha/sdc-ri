package it.org.ieee11073.sdc.dpws.soap;


import org.ieee11073.sdc.dpws.DpwsTest;
import org.ieee11073.sdc.dpws.client.DiscoveryFilterBuilder;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class CommunicationIT extends DpwsTest {
    private DevicePeer devicePeer;
    private ClientPeer clientPeer;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.devicePeer = new DevicePeer();
        this.clientPeer = new ClientPeer();
    }

    @Test
    public void explicitDeviceDiscovery() throws Exception {
        devicePeer.startAsync().awaitRunning();
        //clientPeer.startAsync().awaitRunning();

        //DiscoveryFilterBuilder discoveryFilterBuilder = new DiscoveryFilterBuilder()
        //        .addScope("");
        //clientPeer.getClient().probe(discoveryFilterBuilder.get());
        Thread.sleep(1000000000);
        assertTrue(false);
    }
}
