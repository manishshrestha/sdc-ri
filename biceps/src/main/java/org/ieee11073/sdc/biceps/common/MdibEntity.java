package org.ieee11073.sdc.biceps.common;

import org.ieee11073.sdc.biceps.model.participant.*;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

///**
// * Representation of any MDIB object within a containment tree.
// * <p>
// * Any entity contains a list of states, a descriptor and an optional set of children. {@linkplain MdibEntity} allows
// * read access, only. Write access is established by using {@link MdibEntity}.
// */
public interface MdibEntity {
    MdibVersion getLastChanged();

    /**
     * Obtain a read-only list of the MDIB entity's children.
     *
     * @return Readable list of children currently associated with the entity.
     */
    List<String> getChildren();

    /**
     * Obtain the entity's descriptor
     *
     * @return Descriptor information currently associated with the entity.
     */
    AbstractDescriptor getDescriptor();

    /**
     * Unique name to identify the entity.
     * <p>
     * Short-hand for {@code getDescriptor().getHandle()}
     *
     * @return The entity's unique handle.
     */
    String getHandle();

    /**
     * Obtain the entity's parent.
     *
     * @return An {@link MdibEntity} object for each node that is not direct child of the containment tree root. This
     * applies for implementations of {@link org.ieee11073.sdc.biceps.model.participant.MdsDescriptor}. Otherwise, result is
     * empty.
     */
    Optional<String> getParent();

    /**
     * Obtain a list copy of the MDIB entity's state list.
     *
     * @return List of states currently associated with the entity.
     */
    List<AbstractState> getStates();

    StateAlternative<List<AbstractMultiState>> doIfSingleState(Consumer<AbstractState> consumer);
    StateAlternative<AbstractState> doIfMultiState(Consumer<List<AbstractMultiState>> consumer);

    interface StateAlternative<T> {
        void orElse(Consumer<T> consumer);
    }
}
