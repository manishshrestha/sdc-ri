package org.ieee11073.sdc.biceps.common;

import org.ieee11073.sdc.biceps.model.participant.AbstractContextState;
import org.ieee11073.sdc.biceps.model.participant.AbstractDescriptor;
import org.ieee11073.sdc.biceps.model.participant.AbstractMultiState;
import org.ieee11073.sdc.biceps.model.participant.AbstractState;

import java.util.List;
import java.util.Optional;

/**
 * Registry-based access to {@link MdibEntity} instances.
 * <p>
 * In this case "registry-based" means that there is fast (hash map) access to any {@link MdibEntity} instance of a by
 * any other means read {@link org.ieee11073.sdc.biceps.model.participant.Mdib}.
 */
public interface MdibStorage {
    DescriptionResult apply(MdibDescriptionModifications descriptionModifications);

    StateResult apply(MdibStateModifications stateModifications);

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
     * Retrieve specific descriptor of the hosted {@link org.ieee11073.sdc.biceps.model.participant.Mdib}.
     *
     * @param handle     Handle name of the descriptor.
     *
     * @return Return {@link Optional} of requested descriptor or {@link Optional#empty()} if not found or something
     * went wrong.
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
     * @return Return {@link Optional} of requested state or {@link Optional#empty()} if not found or something
     * went wrong.
     */
    Optional<AbstractState> getState(String handle);

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

    /**
     * Get set of all states of the hosted {@link org.ieee11073.sdc.biceps.model.participant.Mdib}.
     *
     * Collections may be created on function call, hence be careful with performance issues.
     */
    <T extends AbstractMultiState> List<T> getMultiStates(String descriptorHandle, Class<T> stateClass);

    /**
     * Get set of all states of the hosted {@link org.ieee11073.sdc.biceps.model.participant.Mdib}.
     *
     * Collections may be created on function call, hence be careful with performance issues.
     */
    <T extends AbstractMultiState> List<T> getMultiStates(String descriptorHandle);

    /**
     * Get set of all states of the hosted {@link org.ieee11073.sdc.biceps.model.participant.Mdib}.
     *
     * Collections may be created on function call, hence be careful with performance issues.
     */
    List<AbstractContextState> getContextStates(String descriptorHandle);

    interface DescriptionResult {
        List<MdibEntity> getInsertedEntities();

        List<MdibEntity> getUpdatedEntities();

        List<String> getDeletedEntities();
    }

    interface StateResult {
        List<AbstractState> getStates();
    }
}
