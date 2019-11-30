package org.somda.sdc.glue.consumer;

import com.google.common.util.concurrent.Service;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.dpws.service.HostingServiceProxy;

/**
 * SDC consumer device interface.
 * <p>
 * The purpose of {@linkplain SdcRemoteDevice} is to receive SDC data from the network.
 */
public interface SdcRemoteDevice extends Service {

    HostingServiceProxy getHostingServiceProxy();

    RemoteMdibAccess getMdibAccess();

    SetServiceAccess getSetServiceAccess();

    void registerWatchdogObserver(WatchdogObserver watchdogObserver);
    void unregisterWatchdogObserver(WatchdogObserver watchdogObserver);
}
