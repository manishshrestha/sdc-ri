package org.somda.sdc.glue.consumer;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.assistedinject.AssistedInject;

import java.net.URI;

public class SdcRemoteDeviceImpl extends AbstractIdleService implements SdcRemoteDevice {
    private final URI endpointReferenceAddress;

    @AssistedInject
    SdcRemoteDeviceImpl(URI endpointReferenceAddress) {
        this.endpointReferenceAddress = endpointReferenceAddress;
    }

    @Override
    public void registerWatchdogObserver(WatchdogObserver watchdogObserver) {
        checkRunning();
    }

    @Override
    public void unregisterWatchdogObserver(WatchdogObserver watchdogObserver) {
        checkRunning();
    }

    @Override
    protected void startUp() throws Exception {

    }

    @Override
    protected void shutDown() throws Exception {

    }

    private void checkRunning() {
        if (!isRunning()) {
            throw new RuntimeException(String.format("Tried to access a disconnected SDC remote device instance with EPR address %s",
                    endpointReferenceAddress.toString()));
        }
    }
}
