package org.ieee11073.sdc.dpws.client.helper;

import com.google.common.util.concurrent.*;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import org.ieee11073.sdc.dpws.client.ClientConfig;
import org.ieee11073.sdc.dpws.service.HostingServiceProxy;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.WsDiscoveryClient;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.model.ProbeMatchesType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Default implementation of {@link WatchDog}.
 */
public class WatchDogImpl extends AbstractIdleService implements Service, WatchDog {
    private static final Logger LOG = LoggerFactory.getLogger(WatchDogImpl.class);

    private final WsDiscoveryClient wsDiscoveryClient;
    private final Consumer<HostingServiceProxy> watchdogTriggerCallback;
    private final Duration watchdogPeriod;
    private final ScheduledExecutorService scheduler;

    @Inject
    WatchDogImpl(@Assisted WsDiscoveryClient wsDiscoveryClient,
                 @Assisted Consumer<HostingServiceProxy> watchdogTriggerCallback,
                 @Named(ClientConfig.WATCHDOG_PERIOD) Duration watchdogPeriod,
                 @WatchDogScheduler ScheduledExecutorService scheduler) {
        this.wsDiscoveryClient = wsDiscoveryClient;
        this.watchdogTriggerCallback = watchdogTriggerCallback;
        this.watchdogPeriod = watchdogPeriod;
        this.scheduler = scheduler;
    }

    @Override
    protected void startUp() throws Exception {
        LOG.info("Watchdog started.");
    }

    @Override
    protected void shutDown() throws Exception {
        scheduler.shutdownNow();
        LOG.info("Watchdog stopped.");
    }

    @Override
    public void inspect(HostingServiceProxy hostingServiceProxy) {
        if (!isRunning()) {
            LOG.info("WatchDog is not running. Inspection skipped.");
            return;
        }

        LOG.info("Start watchdog for {}", hostingServiceProxy.getEndpointReferenceAddress());
        scheduler.scheduleAtFixedRate(() -> {
            ListenableFuture<ProbeMatchesType> future = wsDiscoveryClient.sendDirectedProbe(hostingServiceProxy,
                    new ArrayList<>(), new ArrayList<>());

            try {
                future.get();
            } catch (Exception e) {
                // Assume device lost on any error
                watchdogTriggerCallback.accept(hostingServiceProxy);

                LOG.info("Watchdog failed to request {}.",
                        hostingServiceProxy.getEndpointReferenceAddress());

                // Stop job by throwing an exception
                throw new RuntimeException("Watchdog timeout; stop schedule.");
            }
        }, watchdogPeriod.getSeconds(), watchdogPeriod.getSeconds(), TimeUnit.SECONDS);
    }
}
