package org.ieee11073.sdc.biceps.common;

import org.ieee11073.sdc.biceps.model.participant.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Container to collect state updates supposed to be applied on an MDIB.
 *
 * The {@linkplain MdibStateModifications} is a fluent interface.
 */
public class MdibStateModifications {
    private final MdibStateModifications.Type changeType;
    private List<AbstractState> states;

    /**
     * Create set.
     */
    public static MdibStateModifications create(Type changeType) {
        return new MdibStateModifications(changeType);
    }

    /**
     * Add a single element to the change set.
     *
     * @throws ClassCastException Is thrown if the element does not match the change type set on {@link #create(Type)}.
     */
    public MdibStateModifications add(AbstractState state) {
        if (!changeType.getChangeBaseClass().isAssignableFrom(state.getClass())) {
            throw new ClassCastException(String.format("Expected added state to be of type %s, but was %s.",
                    changeType.getChangeBaseClass(),
                    state.getClass()));
        }

        states.add(state);
        return this;
    }

    /**
     * Add multiple elements to the change set.
     *
     * @throws ClassCastException Is thrown if one of the elements does not match the change type set on {@link #create(Type)}.
     */
    public <T extends AbstractState> MdibStateModifications addAll(Collection<T> states) {
        states.stream().forEach(state -> add(state));
        return this;
    }

    /**
     * Get the list of states to be updated.
     */
    public List<AbstractState> getStates() {
        return this.states;
    }

    /**
     * Change type designation.
     *
     * In accordance to BICEPS one change set can be of a certain base type. BICEPS distinguishes between
     *
     * - alert changes
     * - component changes
     * - context changes
     * - operation changes
     */
    public enum Type {
        ALERT(AbstractAlertState.class),
        COMPONENT(AbstractDeviceComponentState.class),
        CONTEXT(AbstractContextState.class),
        METRIC(AbstractMetricState.class),
        OPERATION(AbstractOperationState.class);

        private Class<? extends AbstractState> changeBaseClass;

        Type(Class<? extends AbstractState> changeBaseClass) {
            this.changeBaseClass = changeBaseClass;
        }

        Class<? extends AbstractState> getChangeBaseClass() {
            return changeBaseClass;
        }
    }

    private MdibStateModifications(Type changeType) {
        this.changeType = changeType;
        this.states = new ArrayList<>();
    }
}
