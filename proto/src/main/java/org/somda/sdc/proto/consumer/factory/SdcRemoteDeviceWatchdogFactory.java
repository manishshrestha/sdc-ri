package org.somda.sdc.proto.consumer.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.proto.consumer.Consumer;
import org.somda.sdc.proto.consumer.SdcRemoteDeviceWatchdog;
import org.somda.sdc.proto.consumer.WatchdogObserver;

import javax.annotation.Nullable;

/**
 * Factory to create {@linkplain SdcRemoteDeviceWatchdog} instances.
 */
public interface SdcRemoteDeviceWatchdogFactory {
    /**
     * Creates a {@linkplain SdcRemoteDeviceWatchdog} instance.
     *
     * @param initialWatchdogObserver optional initial watchdog observer.
     * @return a new {@link SdcRemoteDeviceWatchdog} instance.
     */
    SdcRemoteDeviceWatchdog create(@Assisted Consumer consumer,
                                   @Assisted @Nullable WatchdogObserver initialWatchdogObserver);
}
