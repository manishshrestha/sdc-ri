package org.ieee11073.sdc.dpws.udp;

import com.google.common.eventbus.Subscribe;
import org.ieee11073.sdc.dpws.DpwsTest;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class UdpMessageQueueServiceTest extends DpwsTest {
    private UdpMessageQueueServiceImpl udpMsgQueue;
    private byte[] actualMessage;
    private Condition condition;
    private Lock lock;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        udpMsgQueue = getInjector().getInstance(UdpMessageQueueServiceImpl.class);
        udpMsgQueue.setUdpBinding(new MockUdpBindingService());
        lock = new ReentrantLock();
        condition = lock.newCondition();
        actualMessage = null;
    }

    @Test
    public void sendAndReceiveMessage() throws Exception {
        String expectedMsg = "sample";
        udpMsgQueue.startAsync().awaitRunning();
        udpMsgQueue.registerUdpMessageQueueObserver(new UdpMessageQueueObserver() {
            @Subscribe
            private void receive(UdpMessage msg) {
                lock.lock();
                try {
                    actualMessage = msg.getData();
                    condition.signalAll();
                } finally {
                    lock.unlock();
                }
            }
        });

        UdpMessage msg = new UdpMessage(expectedMsg.getBytes(), expectedMsg.length());
        udpMsgQueue.sendMessage(msg);
        lock.lock();
        try {
            if (actualMessage == null) {
                condition.await(1, TimeUnit.HOURS);
            }
            assertThat(actualMessage, is(notNullValue(null)));
            assertThat(actualMessage, is(expectedMsg.getBytes()));
        } finally {
            lock.unlock();
        }
    }
}