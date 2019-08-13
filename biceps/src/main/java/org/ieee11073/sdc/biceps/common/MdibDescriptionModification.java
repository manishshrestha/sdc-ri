package org.ieee11073.sdc.biceps.common;

import org.ieee11073.sdc.biceps.model.participant.AbstractDescriptor;
import org.ieee11073.sdc.biceps.model.participant.AbstractState;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Single change item.
 */
public class MdibDescriptionModification {

    final private Type modificationType;
    final private AbstractDescriptor descriptor;
    final private List<? extends AbstractState> states;
    final private String parentHandle;

    public MdibDescriptionModification(Type modificationType,
                                       AbstractDescriptor descriptor,
                                       @Nullable List<? extends AbstractState> states,
                                       @Nullable String parentHandle) {
        this.modificationType = modificationType;
        this.descriptor = descriptor;
        this.states = states == null ? Collections.emptyList() : states;
        this.parentHandle = parentHandle;
    }

    public MdibDescriptionModification(Type modificationType,
                                       AbstractDescriptor descriptor,
                                       @Nullable List<? extends AbstractState> states) {
        this(modificationType, descriptor, states, null);
    }

    public Type getModificationType() {
        return modificationType;
    }

    public AbstractDescriptor getDescriptor() {
        return descriptor;
    }

    public String getHandle() {
        return descriptor.getHandle();
    }

    public List<? extends AbstractState> getStates() {
        return states;
    }

    public Optional<String> getParentHandle() {
        return Optional.ofNullable(parentHandle);
    }

    /**
     * Designates the MDIB description modification type, i.e., insert, update, or delete.
     */
    public enum Type {
        INSERT,
        UPDATE,
        DELETE
    }
}
