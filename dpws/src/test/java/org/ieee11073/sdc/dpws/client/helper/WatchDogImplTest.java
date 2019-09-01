package org.ieee11073.sdc.dpws.client.helper;

import com.google.common.util.concurrent.SettableFuture;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.ieee11073.sdc.dpws.service.HostingServiceProxy;
import org.ieee11073.sdc.dpws.soap.exception.TransportException;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.WsDiscoveryClient;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.model.ProbeMatchesType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import test.org.ieee11073.common.TestLogging;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WatchDogImplTest {
    private WsDiscoveryClient discoveryClientMock;
    private HostingServiceProxy hostingServiceProxyMock;
    private Lock lock;
    private Condition condition;
    private boolean hasFailed;

    @Before
    public void setUp() throws Exception {
        TestLogging.configure();

        lock = new ReentrantLock();
        condition = lock.newCondition();
        hasFailed = false;
        hostingServiceProxyMock = mock(HostingServiceProxy.class);
        discoveryClientMock = mock(WsDiscoveryClient.class);
        SettableFuture<ProbeMatchesType> future = SettableFuture.create();
        future.setException(new TransportException("Request failed."));
        when(discoveryClientMock.sendDirectedProbe(null, null, null))
                .thenReturn(future);
    }

    @Test
    public void inspect() throws Exception {
        WatchDogImpl watchDog = new WatchDogImpl(discoveryClientMock, hostingServiceProxy -> {
            lock.lock();
            try {
                hasFailed = hostingServiceProxy == hostingServiceProxyMock;
                condition.notify();
            } finally {
                lock.unlock();
            }
        }, Duration.ofSeconds(1), Executors.newScheduledThreadPool(1));
        watchDog.startAsync().awaitRunning();

        watchDog.inspect(hostingServiceProxyMock);

        lock.lock();
        try {
            condition.await(2, TimeUnit.SECONDS);
            Assert.assertTrue(hasFailed);
        } catch (InterruptedException e) {
            throw new RuntimeException("Thread unexpectedly interrupted.");
        } finally {
            lock.unlock();
        }
    }
}