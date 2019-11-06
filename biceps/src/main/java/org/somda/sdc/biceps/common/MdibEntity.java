package org.ieee11073.sdc.biceps.common;

import org.ieee11073.sdc.biceps.model.participant.AbstractDescriptor;
import org.ieee11073.sdc.biceps.model.participant.AbstractMultiState;
import org.ieee11073.sdc.biceps.model.participant.AbstractState;
import org.ieee11073.sdc.biceps.model.participant.MdibVersion;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Representation of any MDIB object within a containment tree.
 * <p>
 * Any entity contains a list of states, a descriptor and an optional set of children.
 * {@linkplain MdibEntity} is an immutable class with read access only.
 */
public interface MdibEntity {
    /**
     * Gets the MDIB version of the entity where it has been changed the last time.
     *
     * @return an MDIB version.
     */
    MdibVersion getLastChanged();

    /**
     * Obtain a read-only list of the MDIB entity's child handles.
     *
     * @return readable list of children currently associated with the entity.
     */
    List<String> getChildren();

    /**
     * Obtain the entity's descriptor.
     *
     * @return descriptor information currently associated with the entity.
     */
    AbstractDescriptor getDescriptor();

    /**
     * Obtains the entity's descriptor as a given type.
     *
     * @param theClass the class to cast.
     * @return descriptor information currently associated with the entity.
     */
    <T extends AbstractDescriptor> Optional<T> getDescriptor(Class<T> theClass);

    /**
     * Unique name to identify the entity.
     * <p>
     * Short-hand for {@code getDescriptor().getHandle()}
     *
     * @return the entity's unique handle.
     */
    String getHandle();

    /**
     * Obtain the entity's parent handle.
     *
     * @return the handle of the parent if known, otherwise {@linkplain Optional#empty()}.
     */
    Optional<String> getParent();

    /**
     * Obtain a copy of the MDIB entity's state list.
     *
     * @return list of states currently associated with the entity.
     */
    List<AbstractState> getStates();

    /**
     * Convenience method to execute code if en entity hosts a single-state.
     *
     * @param consumer lambda that is supposed to be executed if the entity is a single state host.
     * @return an alternative to execute if the opposite turns out.
     */
    StateAlternative<List<AbstractMultiState>> doIfSingleState(Consumer<AbstractState> consumer);

    /**
     * Convenience method to execute code if en entity hosts a multi-state.
     *
     * @param consumer lambda that is supposed to be executed if the entity is a multi-state host.
     * @return an alternative to execute if the opposite turns out.
     */
    StateAlternative<AbstractState> doIfMultiState(Consumer<List<AbstractMultiState>> consumer);

    /**
     * Gets the descriptor class.
     * <p>
     * Short-hand for {@code getDescriptor().getClass()}.
     *
     * @return the descriptor class of the entity.
     */
    Class<? extends AbstractDescriptor> getDescriptorClass();

    /**
     * Gets the state class.
     *
     * @return the state class of the entity.
     */
    Class<? extends AbstractState> getStateClass();

    /**
     * An alternative lambda to be executed.
     *
     * @param <T> the state type to accept.
     * @see #doIfSingleState(Consumer)
     * @see #doIfMultiState(Consumer)
     */
    interface StateAlternative<T> {
        void orElse(Consumer<T> consumer);
    }
}
