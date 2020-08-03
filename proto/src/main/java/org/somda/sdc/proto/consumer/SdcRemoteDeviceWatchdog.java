package org.somda.sdc.proto.consumer;

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.client.Client;
import org.somda.sdc.proto.consumer.event.WatchdogMessage;

import javax.annotation.Nullable;
import java.util.concurrent.Future;

public class SdcRemoteDeviceWatchdog extends AbstractIdleService {
    private static final Logger LOG = LogManager.getLogger(SdcRemoteDeviceWatchdog.class);
    private final EventBus eventBus;
    private final Client client;
    private final Consumer consumer;
    @Nullable
    private final WatchdogObserver initialWatchdogObserver;
    private final Logger instanceLogger;
    private Future<?> currentJob = null;

    @AssistedInject
    SdcRemoteDeviceWatchdog(@Assisted Consumer consumer,
                            @Assisted @Nullable WatchdogObserver initialWatchdogObserver,
                            EventBus eventBus,
                            Client client,
                            @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.consumer = consumer;
        this.initialWatchdogObserver = initialWatchdogObserver;
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
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
    }

    @Override
    protected void shutDown() {
    }

    public void postWatchdogMessage(Exception reason) {
        if (isRunning()) {
            eventBus.post(new WatchdogMessage(consumer.getEprAddress(), reason));
        }
    }

    private class WatchdogJob implements Runnable {
        @Override
        public void run() {
//            Duration timeout = watchdogPeriod;
//            boolean watchdogRequestSent = false;
//            for (String serviceId : subscriptions.keySet()) {
//                final Instant start = Instant.now();
//                final SubscribeResult subscribeResult = subscriptions.get(serviceId);
//                final HostedServiceProxy hostedServiceProxy = hostingServiceProxy.getHostedServices().get(serviceId);
//                if (hostedServiceProxy == null) {
//                    instanceLogger.warn("Could not find expected hosted service with id {}", serviceId);
//                    postWatchdogMessage(new Exception(String.format(
//                            "Could not find expected hosted service with id %s", serviceId)));
//                    return;
//                }
//
//                final ListenableFuture<Duration> renewFuture = hostedServiceProxy.getEventSinkAccess()
//                        .renew(subscribeResult.getSubscriptionId(), requestedExpires);
//
//                try {
//                    final Duration grantedExpires = renewFuture.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
//                    if (grantedExpires.compareTo(watchdogPeriod) < 0) {
//                        instanceLogger.warn("Too little time granted for subscription on service {} " +
//                                        "(expected at least {}, got {})",
//                                serviceId, watchdogPeriod, grantedExpires);
//                        postWatchdogMessage(new Exception(String.format(
//                                "Too little time granted for subscription on service %s (expected at least %s, got %s)",
//                                serviceId, watchdogPeriod, grantedExpires)));
//                        return;
//                    }
//                } catch (Exception e) {
//                    instanceLogger.warn("Trying to renew subscription running on service {} failed", serviceId);
//                    postWatchdogMessage(new Exception(String.format(
//                            "Trying to renew subscription running on service %s failed", serviceId), e));
//                    return;
//                }
//
//                final Instant finish = Instant.now();
//                timeout = timeout.minus(Duration.between(start, finish));
//                if (timeout.toMillis() < 0) {
//                    instanceLogger.warn("Watchdog timeout exceeded. " +
//                            "Could not get watchdog triggers served in time.");
//                    postWatchdogMessage(new Exception("Watchdog timeout exceeded. " +
//                            "Could not get watchdog triggers served in time."));
//                    return;
//                }
//
//                watchdogRequestSent = true;
//            }
//
//            if (!watchdogRequestSent) {
//                final Instant start = Instant.now();
//                final ListenableFuture<ProbeMatchesType> probeFuture =
//                        client.directedProbe(hostingServiceProxy.getActiveXAddr());
//                try {
//                    probeFuture.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
//                    final Instant finish = Instant.now();
//                    timeout = timeout.minus(Duration.between(start, finish));
//                } catch (Exception e) {
//                    instanceLogger.warn("Trying to request a directed probe failed");
//                    postWatchdogMessage(new Exception("Trying to request a directed probe failed", e));
//                    return;
//                }
//            }
//
//            if (isRunning() && watchdogExecutor.isRunning()) {
//                currentJob =
//                        watchdogExecutor.get().schedule(new WatchdogJob(), timeout.toMillis(), TimeUnit.MILLISECONDS);
//            } else {
//                currentJob = null;
//                instanceLogger.info(
//                        "WatchdogJob has ended, SdcRemoteDeviceWatchdog ({}) or WatchdogExecutor ({}) have ended",
//                        state(), watchdogExecutor.state()
//                );
//            }
        }
    }
}
