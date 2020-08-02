package org.somda.sdc.proto.consumer.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.dpws.soap.wseventing.SubscribeResult;
import org.somda.sdc.proto.consumer.Consumer;
import org.somda.sdc.proto.consumer.SdcRemoteDeviceWatchdog;
import org.somda.sdc.proto.consumer.WatchdogObserver;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Factory to create {@linkplain SdcRemoteDeviceWatchdog} instances.
 */
public interface SdcRemoteDeviceWatchdogFactory {
    /**
     * Creates a {@linkplain SdcRemoteDeviceWatchdog} instance.
     *
     * @param consumer                the hosting service proxy this watchdog is watching.
     * @param subscriptions           subscriptions used for auto-renew.
     * @param initialWatchdogObserver optional initial watchdog observer.
     * @return a new {@link SdcRemoteDeviceWatchdog} instance.
     */
    SdcRemoteDeviceWatchdog create(@Assisted Consumer consumer,
                                   @Assisted Map<String, SubscribeResult> subscriptions,
                                   @Assisted @Nullable WatchdogObserver initialWatchdogObserver);
}
