package org.ieee11073.sdc.dpws.udp;

import org.ieee11073.sdc.dpws.DpwsConstants;
import org.ieee11073.sdc.dpws.DpwsTest;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.WsDiscoveryConstants;
import org.ieee11073.sdc.dpws.udp.factory.UdpBindingServiceFactory;
import org.junit.Before;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.Assert.*;

public class UdpBindingServiceImplTest extends DpwsTest {
    private Lock lock;
    private Condition condition;
    private UdpMessage actualMessage;
    private UdpBindingServiceFactory factory;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        factory = getInjector().getInstance(UdpBindingServiceFactory.class);
        lock = new ReentrantLock();
        condition = lock.newCondition();
    }

    //@Test
    public void testSendMulticastMessage() throws Exception {
        final byte[] expectedMessage = "SAMPLE".getBytes();
        actualMessage = null;

        UdpBindingService receiver = factory.createUdpBindingService(
                InetAddress.getByName(WsDiscoveryConstants.IPV4_MULTICAST_ADDRESS),
                DpwsConstants.DISCOVERY_PORT,
                DpwsConstants.MAX_UDP_ENVELOPE_SIZE);
        Thread t = new Thread(() -> {
            try {
                UdpBindingService sender = factory.createUdpBindingService(
                        InetAddress.getByName(WsDiscoveryConstants.IPV4_MULTICAST_ADDRESS),
                        DpwsConstants.DISCOVERY_PORT, DpwsConstants.MAX_UDP_ENVELOPE_SIZE);
                sender.startAsync().awaitRunning();
                sender.sendMessage(new UdpMessage(expectedMessage, expectedMessage.length));
            } catch (IOException e) {
                assertTrue(false);
            }
        });

        receiver.startAsync().awaitRunning();
        receiver.setMessageReceiver(msg -> {
            try {
                lock.lock();
                actualMessage = msg;
                condition.signalAll();
            } finally {
                lock.unlock();
            }
        });

        t.start();
        try {
            lock.lock();
            if (actualMessage == null) {
                condition.await(10, TimeUnit.SECONDS);
            }

            assertNotNull(actualMessage);
            byte[] actualBytes = Arrays.copyOf(actualMessage.getData(), actualMessage.getLength());
            assertEquals(Arrays.toString(expectedMessage), Arrays.toString(actualBytes));
        } finally {
            lock.unlock();
        }
    }
}