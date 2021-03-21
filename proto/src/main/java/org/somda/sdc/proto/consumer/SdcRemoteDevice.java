package org.somda.sdc.proto.consumer;

import com.google.common.util.concurrent.Service;
import org.somda.sdc.biceps.common.access.MdibAccess;
import org.somda.sdc.biceps.common.access.MdibAccessObservable;
import org.somda.sdc.biceps.model.message.AbstractSet;
import org.somda.protosdc.proto.model.discovery.DeviceMetadata;

import java.util.function.Consumer;

/**
 * SDC consumer device interface.
 * <p>
 * The purpose of {@linkplain SdcRemoteDevice} is to receive SDC data from the network.
 */
public interface SdcRemoteDevice extends Service {
    /**
     * Gets the hosting service proxy.
     * <p>
     * The hosting service proxy can be used to access the device including metadata and services.
     *
     * @return the hosting service proxy bundled with the SDC remote device instance.
     */
    DeviceMetadata getDeviceMetadata();

    /**
     * Read access to the remote MDIB.
     * <p>
     * The remote MDIB is updated by a background process that processes incoming reports.
     *
     * @return MDIB access to read the remote MDIB.
     */
    MdibAccess getMdibAccess();

    /**
     * Gets an interface to subscribe for MDIB updates.
     *
     * @return the MDIB observable interface.
     */
    MdibAccessObservable getMdibAccessObservable();

    /**
     * Gets a set service invoker access.
     * <p>
     * Please note that the set service access only works if the context and/or set service are available from
     * the remote device.
     * If not set service exists, any call to the {@link SetServiceAccess} interface results in an immediately cancelled
     * future.
     *
     * @return a set service invoker access interface.
     * @see SetServiceAccess#invoke(AbstractSet, Class)
     * @see SetServiceAccess#invoke(AbstractSet, Consumer, Class)
     */
    SetServiceAccess getSetServiceAccess();

    /**
     * In order to get notified on disconnect events, this function attaches a watchdog observer.
     *
     * @param watchdogObserver the watchdog callback interface.
     */
    void registerWatchdogObserver(WatchdogObserver watchdogObserver);

    /**
     * Removes a watchdog observer.
     *
     * @param watchdogObserver the watchdog observer to remove.
     */
    void unregisterWatchdogObserver(WatchdogObserver watchdogObserver);
}
