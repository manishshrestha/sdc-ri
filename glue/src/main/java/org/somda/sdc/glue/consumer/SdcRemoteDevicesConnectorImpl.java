package org.somda.sdc.glue.consumer;

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.glue.guice.Consumer;

import javax.inject.Inject;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class SdcRemoteDevicesConnectorImpl implements SdcRemoteDevicesConnector {
    private ListeningExecutorService executorService;
    private Map<URI, SdcRemoteDevice> sdcRemoteDevices;
    private EventBus eventBus;

    @Inject
    SdcRemoteDevicesConnectorImpl(@Consumer ListeningExecutorService executorService,
                                  ConcurrentHashMap sdcRemoteDevices,
                                  EventBus eventBus) {
        this.executorService = executorService;
        this.sdcRemoteDevices = sdcRemoteDevices;
        this.eventBus = eventBus;
    }

    @Override
    public ListenableFuture<SdcRemoteDevice> connect(HostingServiceProxy hostingServiceProxy,
                                                     ConnectConfiguration connectConfiguration) {
        // precheck: necessary services present?
        return executorService.submit(() -> {
            // create report processor
            // subscribe to given services
            // get mdib
            // optionally get context states
            // apply mdib on report processor
            // create sdc remote device
            // add to sdcRemoteDevices
            // return sdc remote device
            return new SdcRemoteDevice() {
                @Override
                public void registerWatchdogObserver(WatchdogObserver watchdogObserver) {

                }

                @Override
                public void unregisterWatchdogObserver(WatchdogObserver watchdogObserver) {

                }
            };
        });
    }

    @Override
    public void disconnect(URI eprAddress) {
        SdcRemoteDevice sdcRemoteDevice = sdcRemoteDevices.remove(eprAddress);
        if (sdcRemoteDevice != null) {
            // invalidate sdcRemoteDevice
            // unsubscribe everything

        }
    }

    @Override
    public Collection<SdcRemoteDevice> getConnectedDevices() {
        return new ArrayList<>(sdcRemoteDevices.values());
    }

    @Override
    public Optional<SdcRemoteDevice> getConnectedDevice(URI eprAddress) {
        return Optional.empty();
    }

    @Override
    public void registerObserver(SdcRemoteDevicesObserver observer) {
        eventBus.register(observer);
    }

    @Override
    public void unregisterObserver(SdcRemoteDevicesObserver observer) {
        eventBus.unregister(observer);
    }
}
