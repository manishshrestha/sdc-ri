package it.org.ieee11073.sdc.dpws.udp;

import org.ieee11073.sdc.dpws.DpwsConstants;
import org.ieee11073.sdc.dpws.DpwsTest;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.WsDiscoveryConstants;
import org.ieee11073.sdc.dpws.udp.UdpBindingService;
import org.ieee11073.sdc.dpws.udp.UdpMessage;
import org.ieee11073.sdc.dpws.udp.factory.UdpBindingServiceFactory;
import org.junit.Before;
import org.junit.Test;
import test.org.ieee11073.common.FutureCondition;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.Assert.*;

public class UdpBindingServiceImplIT extends DpwsTest {
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

    @Test
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
            } catch (Exception e) {
                assertTrue(false);
            }
        });

        receiver.setMessageReceiver(msg -> {
            try {
                lock.lock();
                actualMessage = msg;
                condition.signalAll();
            } finally {
                lock.unlock();
            }
        });
        receiver.startAsync().awaitRunning();

        t.start();
        try {
            lock.lock();
            long wait = 10000;
            long tStart = System.currentTimeMillis();
            while (wait > 0) {
                if (actualMessage != null) {
                    break;
                }
                condition.await(wait, TimeUnit.MILLISECONDS);
                wait -= System.currentTimeMillis() - tStart;
            }

            assertNotNull(actualMessage);
            byte[] actualBytes = Arrays.copyOf(actualMessage.getData(), actualMessage.getLength());
            assertEquals(Arrays.toString(expectedMessage), Arrays.toString(actualBytes));
        } finally {
            lock.unlock();
        }
    }

    @Test
    public void testSendMulticastMessageWithReply() throws Exception {
        final String expectedRequestStr = "REQUEST";
        final String expectedResponseStr = "RESPONSE";
        final byte[] expectedRequestBytes = expectedRequestStr.getBytes();
        final byte[] expectedResponseBytes = expectedResponseStr.getBytes();

        FutureCondition<String> futureCondition = new FutureCondition<>();

        final UdpBindingService senderReceiver1 = factory.createUdpBindingService(
                InetAddress.getByName(WsDiscoveryConstants.IPV4_MULTICAST_ADDRESS),
                DpwsConstants.DISCOVERY_PORT,
                DpwsConstants.MAX_UDP_ENVELOPE_SIZE);
        senderReceiver1.setMessageReceiver(udpMessage -> {
            if (udpMessage.toString().equals(expectedResponseStr)) {
                futureCondition.setResult(udpMessage.toString());
            }
        });

        final UdpBindingService senderReceiver2 = factory.createUdpBindingService(
                InetAddress.getByName(WsDiscoveryConstants.IPV4_MULTICAST_ADDRESS),
                DpwsConstants.DISCOVERY_PORT,
                DpwsConstants.MAX_UDP_ENVELOPE_SIZE);
        senderReceiver2.setMessageReceiver(udpMessage -> {
            if (udpMessage.toString().equals(expectedRequestStr)) {
                Thread t = new Thread(() -> {
                    try {
                        senderReceiver2.sendMessage(new UdpMessage(expectedResponseBytes, expectedResponseBytes.length, udpMessage.getHost(), udpMessage.getPort()));
                    } catch (IOException e) {
                        assertTrue(false);
                    }
                });
                t.start();
            }
        });

        senderReceiver1.startAsync().awaitRunning();
        senderReceiver2.startAsync().awaitRunning();

        Thread t = new Thread(() -> {
            try {
                senderReceiver1.sendMessage(new UdpMessage(expectedRequestBytes, expectedRequestBytes.length));
            } catch (IOException e) {
                assertTrue(false);
            }
        });
        t.start();

        assertEquals(expectedResponseStr, futureCondition.get(10, TimeUnit.SECONDS).substring(0, expectedResponseStr.length() ));
    }
}