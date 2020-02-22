package org.somda.sdc.glue.provider;

import com.google.common.util.concurrent.Service;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.dpws.device.Device;
import org.somda.sdc.glue.provider.sco.OperationInvocationReceiver;

import java.util.Collection;

/**
 * Context data passed to {@linkplain SdcDevicePlugin} in order to access {@linkplain SdcDevice} data.
 */
public interface SdcDeviceContext {
    /**
     * Gets the encapsulated device of an {@linkplain SdcDevice}.
     * <p>
     * <em>Do not call any startup or shutdown functions from that device reference as it is still managed by the
     * enclosing {@link SdcDevice} instance that provides this context.</em>
     *
     * @return the DPWS device.
     */
    Device getDevice();

    /**
     * Gets the {@linkplain LocalMdibAccess} passed to the {@linkplain SdcDevice} constructor.
     *
     * @return the local MDIB access.
     */
    LocalMdibAccess getLocalMdibAccess();

    /**
     * Gets a collection of {@linkplain OperationInvocationReceiver} instances passed to the {@linkplain SdcDevice} constructor.
     *
     * @return an unmodifiable {@link OperationInvocationReceiver} collection.
     */
    Collection<OperationInvocationReceiver> getOperationInvocationReceivers();

    /**
     * Gets the enclosing {@link SdcDevice} service state.
     *
     * @return {@link SdcDevice#state()} of the enclosing SDC device.
     */
    Service.State getServiceState();
}
