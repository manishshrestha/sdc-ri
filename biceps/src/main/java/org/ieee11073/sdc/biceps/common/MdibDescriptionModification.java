package org.ieee11073.sdc.biceps.common;

import org.ieee11073.sdc.biceps.model.participant.AbstractDescriptor;
import org.ieee11073.sdc.biceps.model.participant.AbstractState;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * Single change item.
 */
public class MdibDescriptionModification {

    final private Type modificationType;
    final private AbstractDescriptor descriptor;
    final private List<? extends AbstractState> states;

    public MdibDescriptionModification(Type modificationType,
                                       AbstractDescriptor descriptor,
                                       @Nullable List<? extends AbstractState> states) {
        this.modificationType = modificationType;
        this.descriptor = descriptor;
        this.states = states == null ? Collections.emptyList() : states;
    }

    public Type getModificationType() {
        return modificationType;
    }

    public AbstractDescriptor getDescriptor() {
        return descriptor;
    }

    public List<? extends AbstractState> getStates() {
        return states;
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
