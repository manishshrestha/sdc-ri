package org.somda.sdc.biceps.common;

import org.somda.sdc.biceps.model.participant.AbstractDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Single change item.
 */
public class MdibDescriptionModification {

    final private Type modificationType;
    final private AbstractDescriptor descriptor;
    final private List<AbstractState> states;
    final private String parentHandle;

    public MdibDescriptionModification(Type modificationType,
                                       AbstractDescriptor descriptor,
                                       @Nullable List<AbstractState> states,
                                       @Nullable String parentHandle) {
        this.modificationType = modificationType;
        this.descriptor = descriptor;
        this.states = states == null ? new ArrayList<>() : new ArrayList<>(states);
        this.parentHandle = parentHandle;
    }

    public MdibDescriptionModification(Type modificationType,
                                       AbstractDescriptor descriptor,
                                       @Nullable List<AbstractState> states) {
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

    public List<AbstractState> getStates() {
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
