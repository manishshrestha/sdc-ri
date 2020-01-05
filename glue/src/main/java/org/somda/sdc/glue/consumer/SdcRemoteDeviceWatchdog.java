package org.somda.sdc.glue.consumer;

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.slf4j.Logger;
import org.somda.sdc.dpws.client.Client;
import org.somda.sdc.dpws.service.HostedServiceProxy;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ProbeMatchesType;
import org.somda.sdc.dpws.soap.wseventing.SubscribeResult;
import org.somda.sdc.glue.consumer.event.WatchdogMessage;
import org.somda.sdc.glue.consumer.helper.LogPrepender;
import org.somda.sdc.glue.guice.WatchdogScheduledExecutor;

import javax.annotation.Nullable;
import javax.inject.Named;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Sends WS-Eventing Renew or DirectedProbe requests and informs in case of an error.
 * <p>
 * {@linkplain SdcRemoteDeviceWatchdog} is a Guava service that requests a remote device in a given periodicity once
 * started.
 * <ul>
 * <li>If at least one subscription exists, {@linkplain SdcRemoteDeviceWatchdog} tries to renew this subscription in order
 * to check if the remote device is still reachable.
 * <li>If multiple subscriptions exist, all are renewed.
 * <li>If no subscription exists, {@linkplain SdcRemoteDeviceWatchdog} sends a directed probe.
 * </ul>
 * The automatic renew spares enabling auto-renew for subscriptions.
 * The requested expires value for Subscribe requests is {@code ConsumerConfig#WATCHDOG_PERIOD * 3}.
 * <p>
 * Watchdog events are distributed only if the service is running.
 *
 * @see ConsumerConfig#WATCHDOG_PERIOD
 */
public class SdcRemoteDeviceWatchdog extends AbstractIdleService {
    private final Logger LOG;
    private final HostingServiceProxy hostingServiceProxy;
    private final Map<String, SubscribeResult> subscriptions;
    private final ScheduledExecutorService watchdogExecutor;
    private final Duration watchdogPeriod;
    private final Duration requestedExpires;
    private final EventBus eventBus;
    private final Client client;

    @AssistedInject
    SdcRemoteDeviceWatchdog(@Assisted HostingServiceProxy hostingServiceProxy,
                            @Assisted Map<String, SubscribeResult> subscriptions,
                            @Assisted @Nullable WatchdogObserver initialWatchdogObserver,
                            @WatchdogScheduledExecutor ScheduledExecutorService watchdogExecutor,
                            @Named(ConsumerConfig.WATCHDOG_PERIOD) Duration watchdogPeriod,
                            EventBus eventBus,
                            Client client) {
        this.LOG = LogPrepender.getLogger(hostingServiceProxy, SdcRemoteDeviceWatchdog.class);
        this.hostingServiceProxy = hostingServiceProxy;
        this.subscriptions = new HashMap<>(subscriptions);
        this.watchdogExecutor = watchdogExecutor;
        this.watchdogPeriod = watchdogPeriod;
        this.requestedExpires = watchdogPeriod.multipliedBy(3);
        this.eventBus = eventBus;
        this.client = client;

        if (initialWatchdogObserver != null) {
            registerObserver(initialWatchdogObserver);
        }
    }

    /**
     * Registers a watchdog observer.
     *
     * @param watchdogObserver the watchdog observer.
     */
    public void registerObserver(WatchdogObserver watchdogObserver) {
        eventBus.register(watchdogObserver);
    }

    /**
     * Unregisters a watchdog observer.
     *
     * @param watchdogObserver the watchdog observer.
     */
    public void unregisterObserver(WatchdogObserver watchdogObserver) {
        eventBus.unregister(watchdogObserver);
    }

    @Override
    protected void startUp() {
        watchdogExecutor.schedule(new WatchdogJob(), watchdogPeriod.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    protected void shutDown() {
    }

    private void postWatchdogMessage(Exception reason) {
        if (isRunning()) {
            eventBus.post(new WatchdogMessage(hostingServiceProxy.getActiveXAddr(), reason));
        }
    }

    private class WatchdogJob implements Runnable {
        @Override
        public void run() {
            Duration timeout = watchdogPeriod;
            boolean watchdogRequestSent = false;
            for (String serviceId : subscriptions.keySet()) {
                final Instant start = Instant.now();
                final SubscribeResult subscribeResult = subscriptions.get(serviceId);
                final HostedServiceProxy hostedServiceProxy = hostingServiceProxy.getHostedServices().get(serviceId);
                if (hostedServiceProxy == null) {
                    LOG.warn("Could not find expected hosted service with id {}", serviceId);
                    postWatchdogMessage(new Exception(String.format("Could not find expected hosted service with id %s", serviceId)));
                    return;
                }

                final ListenableFuture<Duration> renewFuture = hostedServiceProxy.getEventSinkAccess()
                        .renew(subscribeResult.getSubscriptionId(), requestedExpires);

                try {
                    final Duration grantedExpires = renewFuture.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
                    if (grantedExpires.compareTo(watchdogPeriod) < 0) {
                        LOG.warn("Too little time granted for subscription on service {} (expected at least {}, got {})",
                                serviceId, watchdogPeriod, grantedExpires);
                        postWatchdogMessage(new Exception(String.format("Too little time granted for subscription on service %s (expected at least %s, got %s)",
                                serviceId, watchdogPeriod, grantedExpires)));
                        return;
                    }
                } catch (Exception e) {
                    LOG.warn("Trying to renew subscription running on service {} failed", serviceId);
                    postWatchdogMessage(new Exception(String.format("Trying to renew subscription running on service %s failed", serviceId), e));
                    return;
                }

                final Instant finish = Instant.now();
                timeout = timeout.minus(Duration.between(start, finish));
                if (timeout.toMillis() < 0) {
                    LOG.warn("Watchdog timeout exceeded. Could not get watchdog triggers served in time.");
                    postWatchdogMessage(new Exception("Watchdog timeout exceeded. Could not get watchdog triggers served in time."));
                    return;
                }

                watchdogRequestSent = true;
            }

            if (!watchdogRequestSent) {
                final Instant start = Instant.now();
                final ListenableFuture<ProbeMatchesType> probeFuture = client.directedProbe(hostingServiceProxy.getActiveXAddr());
                try {
                    probeFuture.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
                    final Instant finish = Instant.now();
                    timeout = timeout.minus(Duration.between(start, finish));
                } catch (Exception e) {
                    LOG.warn("Trying to request a directed probe failed");
                    postWatchdogMessage(new Exception("Trying to request a directed probe failed", e));
                    return;
                }
            }

            if (isRunning()) {
                watchdogExecutor.schedule(new WatchdogJob(), timeout.toMillis(), TimeUnit.MILLISECONDS);
            }
        }
    }
}
