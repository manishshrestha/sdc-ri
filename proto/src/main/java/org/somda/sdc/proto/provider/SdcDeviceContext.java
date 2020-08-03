package org.somda.sdc.proto.provider;

import com.google.common.util.concurrent.Service;
import org.somda.sdc.biceps.model.participant.AbstractState;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.proto.discovery.provider.TargetService;
import org.somda.sdc.proto.provider.sco.OperationInvocationReceiver;

import java.util.Collection;
import java.util.List;

/**
 * Context data passed to {@linkplain SdcDevicePlugin} in order to access {@linkplain SdcDevice} data.
 */
public interface SdcDeviceContext {
    /**
     * Gets the {@linkplain LocalMdibAccess} passed to the {@linkplain SdcDevice} constructor.
     *
     * @return the local MDIB access.
     */
    LocalMdibAccess getLocalMdibAccess();

    /**
     * Gets a collection of {@linkplain OperationInvocationReceiver} instances
     * passed to the {@linkplain SdcDevice} constructor.
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

    /**
     * Sends a periodic state report.
     * <p>
     * This function does not control periodicity.
     * Periodicity has to be controlled by the calling function.
     * <p>
     * Note that only one report type is supported per call, e.g. it is not possible to mix metric and context states.
     * In accordance with the SDC, the following state types have to be sent in separate reports:
     * <ul>
     * <li>Metric states (every subclass of {@link org.somda.sdc.biceps.model.participant.AbstractMetricState})
     * <li>Alert states (every subclass of {@link org.somda.sdc.biceps.model.participant.AbstractAlertState})
     * <li>Context states (every subclass of {@link org.somda.sdc.biceps.model.participant.AbstractContextState})
     * <li>Operational states (every subclass of {@link org.somda.sdc.biceps.model.participant.AbstractOperationState})
     * <li>Component states (every subclass of
     * {@link org.somda.sdc.biceps.model.participant.AbstractDeviceComponentState})
     * </ul>
     *
     * @param states      the states that are supposed to be notified.
     * @param mdibVersion the MDIB version the report belongs to.
     * @param <T>         the state type that.
     */
    <T extends AbstractState> void sendPeriodicStateReport(List<T> states, MdibVersion mdibVersion);

    /**
     * Gets the target service to alter scopes and physical addresses.
     * @return the target service instance.
     */
    TargetService getTargetService();
}
