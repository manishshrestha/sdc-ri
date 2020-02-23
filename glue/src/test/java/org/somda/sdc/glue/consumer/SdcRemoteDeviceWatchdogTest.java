package org.somda.sdc.glue.consumer;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.Futures;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.somda.sdc.dpws.client.Client;
import org.somda.sdc.common.util.ExecutorWrapperService;
import org.somda.sdc.dpws.model.ThisDeviceType;
import org.somda.sdc.dpws.model.ThisModelType;
import org.somda.sdc.dpws.service.EventSinkAccess;
import org.somda.sdc.dpws.service.HostedServiceProxy;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.dpws.service.factory.HostingServiceFactory;
import org.somda.sdc.dpws.soap.RequestResponseClient;
import org.somda.sdc.dpws.soap.wseventing.SubscribeResult;
import org.somda.sdc.glue.UnitTestUtil;
import org.somda.sdc.glue.consumer.event.WatchdogMessage;
import org.somda.sdc.glue.consumer.factory.SdcRemoteDeviceWatchdogFactory;
import org.somda.sdc.glue.guice.WatchdogScheduledExecutor;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SdcRemoteDeviceWatchdogTest {
    private Injector injector;
    private ScheduledExecutorService mockExecutor;
    private SdcRemoteDeviceWatchdogFactory watchdogFactory;
    private HostingServiceProxy hostingServiceProxy;
    private ArgumentCaptor<Runnable> jobCaptor;
    private String serviceId1;
    private HostedServiceProxy hostedServiceProxy1;
    private String serviceId2;
    private HostedServiceProxy hostedServiceProxy2;
    private Client mockClient;
    private URI eprAddress;
    private URI xAddr;
    private EventSinkAccess mockEventSinkAccess1;
    private EventSinkAccess mockEventSinkAccess2;
    private Duration watchdogPeriod;

    @BeforeEach
    void beforeEach() {
        jobCaptor = ArgumentCaptor.forClass(Runnable.class);
        mockExecutor = mock(ScheduledExecutorService.class);
        mockClient = mock(Client.class);
        injector = new UnitTestUtil().createInjectorWithOverrides(new AbstractModule() {
            @Override
            protected void configure() {
                bind(new TypeLiteral<ExecutorWrapperService<ScheduledExecutorService>>(){})
                        .annotatedWith(WatchdogScheduledExecutor.class)
                        .toInstance(new ExecutorWrapperService<>(() -> mockExecutor, "WatchdogScheduledExecutorMock"));
                bind(Client.class)
                        .toInstance(mockClient);
            }
        });

        // start required thread pool(s)
        injector.getInstance(Key.get(
                new TypeLiteral<ExecutorWrapperService<ScheduledExecutorService>>(){},
                WatchdogScheduledExecutor.class
        )).startAsync().awaitRunning();

        hostedServiceProxy1 = mock(HostedServiceProxy.class);
        mockEventSinkAccess1 = mock(EventSinkAccess.class);
        when(hostedServiceProxy1.getEventSinkAccess()).thenReturn(mockEventSinkAccess1);
        hostedServiceProxy2 = mock(HostedServiceProxy.class);
        mockEventSinkAccess2 = mock(EventSinkAccess.class);
        when(hostedServiceProxy2.getEventSinkAccess()).thenReturn(mockEventSinkAccess2);

        Map<String, HostedServiceProxy> hostedServices = new HashMap<>();
        serviceId1 = "hostedServiceProxy1";
        hostedServices.put(serviceId1, hostedServiceProxy1);
        serviceId2 = "hostedServiceProxy2";
        hostedServices.put(serviceId2, hostedServiceProxy2);

        eprAddress = URI.create("urn:uuid:441dfbea-40e5-406e-b2c4-154d3b8430bf");
        xAddr = URI.create("http://xAddr/");
        hostingServiceProxy = injector.getInstance(HostingServiceFactory.class).createHostingServiceProxy(
                eprAddress,
                Collections.emptyList(),
                mock(ThisDeviceType.class),
                mock(ThisModelType.class),
                hostedServices,
                0,
                mock(RequestResponseClient.class),
                xAddr);

        watchdogFactory = injector.getInstance(SdcRemoteDeviceWatchdogFactory.class);
        watchdogPeriod = injector.getInstance(Key.get(Duration.class, Names.named(ConsumerConfig.WATCHDOG_PERIOD)));
    }

    @Test
    void watchdogExpiresTooShort() {
        WatchdogSpy watchdogSpy = new WatchdogSpy();
        Map<String, SubscribeResult> subscribeResults = new HashMap<>();
        String subscriptionId1 = "subId1";
        subscribeResults.put(serviceId1, new SubscribeResult(subscriptionId1, Duration.ZERO));
        final SdcRemoteDeviceWatchdog watchdog = watchdogFactory.createSdcRemoteDeviceWatchdog(hostingServiceProxy,
                subscribeResults, watchdogSpy);
        watchdog.startAsync().awaitRunning();
        verify(mockExecutor).schedule(jobCaptor.capture(), any(Long.class), any(TimeUnit.class));
        jobCaptor.getValue().run();

        final Duration expires = watchdogPeriod.minus(Duration.ofSeconds(1));
        when(mockEventSinkAccess1.renew(eq(subscriptionId1), any(Duration.class)))
                .thenReturn(Futures.immediateFuture(expires));
        assertTrue(watchdogSpy.getLastWatchdogMessage().isPresent());
    }

    @Test
    void watchdogRenewFails() {
        WatchdogSpy watchdogSpy = new WatchdogSpy();
        Map<String, SubscribeResult> subscribeResults = new HashMap<>();
        String subscriptionId1 = "subId1";
        subscribeResults.put(serviceId1, new SubscribeResult(subscriptionId1, Duration.ZERO));
        final SdcRemoteDeviceWatchdog watchdog = watchdogFactory.createSdcRemoteDeviceWatchdog(hostingServiceProxy,
                subscribeResults, watchdogSpy);
        watchdog.startAsync().awaitRunning();
        verify(mockExecutor).schedule(jobCaptor.capture(), any(Long.class), any(TimeUnit.class));
        when(mockEventSinkAccess1.renew(eq(subscriptionId1), any(Duration.class)))
                .thenReturn(Futures.immediateCancelledFuture());

        jobCaptor.getValue().run();
        assertTrue(watchdogSpy.getLastWatchdogMessage().isPresent());
    }


    @Test
    void watchdogDirectedProbeFails() {
        WatchdogSpy watchdogSpy = new WatchdogSpy();
        Map<String, SubscribeResult> subscribeResults = new HashMap<>();
        String subscriptionId1 = "subId1";
        subscribeResults.put(serviceId1, new SubscribeResult(subscriptionId1, Duration.ZERO));
        final SdcRemoteDeviceWatchdog watchdog = watchdogFactory.createSdcRemoteDeviceWatchdog(hostingServiceProxy,
                subscribeResults, watchdogSpy);
        watchdog.startAsync().awaitRunning();
        verify(mockExecutor).schedule(jobCaptor.capture(), any(Long.class), any(TimeUnit.class));
        when(mockEventSinkAccess1.renew(eq(subscriptionId1), any(Duration.class)))
                .thenReturn(Futures.immediateCancelledFuture());
        jobCaptor.getValue().run();

        assertTrue(watchdogSpy.getLastWatchdogMessage().isPresent());
    }

    @Test
    void testAutoRenew() {
        WatchdogSpy watchdogSpy = new WatchdogSpy();
        Map<String, SubscribeResult> subscribeResults = new HashMap<>();
        String subscriptionId1 = "subId1";
        subscribeResults.put(serviceId1, new SubscribeResult(subscriptionId1, Duration.ZERO));
        String subscriptionId2 = "subId2";
        subscribeResults.put(serviceId2, new SubscribeResult(subscriptionId2, Duration.ZERO));
        final SdcRemoteDeviceWatchdog watchdog = watchdogFactory.createSdcRemoteDeviceWatchdog(hostingServiceProxy,
                subscribeResults, watchdogSpy);

        verifyZeroInteractions(mockExecutor);
        watchdog.startAsync().awaitRunning();
        verify(mockExecutor).schedule(jobCaptor.capture(), any(Long.class), any(TimeUnit.class));

        final Duration expires = watchdogPeriod.plus(Duration.ofSeconds(1));
        when(mockEventSinkAccess1.renew(eq(subscriptionId1), any(Duration.class)))
                .thenReturn(Futures.immediateFuture(expires));
        when(mockEventSinkAccess2.renew(eq(subscriptionId2), any(Duration.class)))
                .thenReturn(Futures.immediateFuture(expires));

        jobCaptor.getValue().run();
        verifyZeroInteractions(mockClient);

        assertTrue(watchdogSpy.getLastWatchdogMessage().isEmpty());
    }

    @Test
    void testReschedule() {
        Map<String, SubscribeResult> subscribeResults = new HashMap<>();
        final SdcRemoteDeviceWatchdog watchdog = watchdogFactory.createSdcRemoteDeviceWatchdog(hostingServiceProxy,
                subscribeResults, null);

        verifyZeroInteractions(mockExecutor);
        watchdog.startAsync().awaitRunning();
        verify(mockExecutor).schedule(jobCaptor.capture(), any(Long.class), any(TimeUnit.class));

        jobCaptor.getValue().run();
        verify(mockExecutor).schedule(jobCaptor.capture(), any(Long.class), any(TimeUnit.class));

        watchdog.stopAsync().awaitTerminated();
        jobCaptor.getValue().run();
        verifyZeroInteractions(mockExecutor);
    }

    @Test
    void testDirectedProbe() {
        Map<String, SubscribeResult> subscribeResults = new HashMap<>();
        final SdcRemoteDeviceWatchdog watchdog = watchdogFactory.createSdcRemoteDeviceWatchdog(hostingServiceProxy,
                subscribeResults, null);

        verifyZeroInteractions(mockExecutor);
        watchdog.startAsync().awaitRunning();
        verify(mockExecutor).schedule(jobCaptor.capture(), any(Long.class), any(TimeUnit.class));
        jobCaptor.getValue().run();
        verify(mockClient).directedProbe(eq(xAddr));
    }

    private class WatchdogSpy implements WatchdogObserver {
        private WatchdogMessage lastWatchdogMessage = null;

        @Subscribe
        void onConnectionLoss(WatchdogMessage watchdogMessage) {
            lastWatchdogMessage = watchdogMessage;
        }

        public Optional<WatchdogMessage> getLastWatchdogMessage() {
            return Optional.ofNullable(lastWatchdogMessage);
        }
    }
}