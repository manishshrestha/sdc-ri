package org.ieee11073.sdc.biceps.common.access;

import org.ieee11073.sdc.biceps.common.MdibEntity;
import org.ieee11073.sdc.biceps.common.MdibVersion;
import org.ieee11073.sdc.biceps.model.participant.AbstractContextState;
import org.ieee11073.sdc.biceps.model.participant.AbstractDescriptor;
import org.ieee11073.sdc.biceps.model.participant.AbstractState;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Read access to MDIB storage.
 */
public interface MdibAccess {

    MdibVersion getMdibVersion();

    BigInteger getMdDescriptionVersion();

    BigInteger getMdStateVersion();

    /**
     * Retrieve specific descriptor of the hosted {@link org.ieee11073.sdc.biceps.model.participant.Mdib}.
     *
     * @param handle     Handle name of the descriptor.
     * @param descrClass Class to cast to. If cast fails, {@link Optional#empty()} will be returned.
     *
     * @return Return {@link Optional} of requested descriptor or {@link Optional#empty()} if not found or something
     * went wrong.
     */
    <T extends AbstractDescriptor> Optional<T> getDescriptor(String handle, Class<T> descrClass);

    /**
     * todo DGr comment
     * @param handle
     * @return
     */
    Optional<AbstractDescriptor> getDescriptor(String handle);

    /**
     * Get an {@link MdibEntity} object with a specific handle.
     */
    Optional<MdibEntity> getEntity(String handle);

    /**
     * Get all {@link MdibEntity} objects that are root elements, i.e., hosting descriptors of type
     * {@link org.ieee11073.sdc.biceps.model.participant.MdsDescriptor}.
     */
    List<MdibEntity> getRootEntities();

    /**
     * Retrieve specific state of the hosted {@link org.ieee11073.sdc.biceps.model.participant.Mdib}.
     *
     * @param handle     The state or descriptor handle of the state to request (descriptor handle is used at single
     *                   states).
     * @param stateClass Class to cast to. If cast fails, {@link Optional#empty()} will be returned.
     *
     * @return Return {@link Optional} of requested state or {@link Optional#empty()} if not found or something
     * went wrong.
     */
    <T extends AbstractState> Optional<T> getState(String handle, Class<T> stateClass);

    /**
     * Get set of all states of the hosted {@link org.ieee11073.sdc.biceps.model.participant.Mdib}.
     *
     * Collections may be created on function call, hence be careful with performance issues.
     */
    <T extends AbstractContextState> List<T> getContextStates(String descriptorHandle, Class<T> stateClass);

    <T extends AbstractDescriptor> Collection<MdibEntity> findEntitiesByType(Class<T> type);

    <T extends AbstractDescriptor> List<MdibEntity> getChildrenByType(String handle, Class<T> type);

}
