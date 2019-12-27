package org.somda.sdc.biceps.common;

import org.somda.sdc.biceps.common.event.*;
import org.somda.sdc.biceps.model.participant.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Container to collect state updates supposed to be applied on an MDIB.
 * <p>
 * The {@linkplain MdibStateModifications} is a fluent interface.
 */
public class MdibStateModifications {
    private final MdibStateModifications.Type changeType;
    private List<AbstractState> states;

    /**
     * Creates a set.
     *
     * @param changeType the change type to be applied for the set.
     * @return a new {@link MdibStateModifications} object.
     */
    public static MdibStateModifications create(Type changeType) {
        return new MdibStateModifications(changeType);
    }

    /**
     * Creates a set with initial capacity.
     *
     * @param changeType      the change type to be applied for the set.
     * @param initialCapacity the number of pre-allocated elements hold by this modifications set.
     * @return a new {@link MdibStateModifications} object.
     */
    public static MdibStateModifications create(Type changeType, int initialCapacity) {
        return new MdibStateModifications(changeType, initialCapacity);
    }

    /**
     * Add a single element to the change set.
     *
     * @param state the state to add.
     * @return this object for fluent access.
     * @throws ClassCastException if the element does not match the change type that has been set on {@link #create(Type)}.
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
     * @param states the states to set.
     * @param <T>    any state type.
     * @return this object for fluent access.
     * @throws ClassCastException if at least one of the elements does not match the change type that has been set on {@link #create(Type)}.
     */
    public <T extends AbstractState> MdibStateModifications addAll(Collection<T> states) {
        states.stream().forEach(state -> add(state));
        return this;
    }

    public List<AbstractState> getStates() {
        return this.states;
    }

    public Type getChangeType() {
        return changeType;
    }

    /**
     * Flushes added states.
     */
    public void clear() {
        this.states.clear();
    }

    /**
     * Change type designation.
     * <p>
     * In accordance to BICEPS a change set can be of one certain base type at a time. BICEPS distinguishes between
     * <ul>
     * <li>alert changes
     * <li>component changes
     * <li>context changes
     * <li>metric changes
     * <li>operation changes
     * </ul>
     */
    public enum Type {
        ALERT(AbstractAlertState.class, AlertStateModificationMessage.class),
        COMPONENT(AbstractDeviceComponentState.class, ComponentStateModificationMessage.class),
        CONTEXT(AbstractContextState.class, ContextStateModificationMessage.class),
        METRIC(AbstractMetricState.class, MetricStateModificationMessage.class),
        OPERATION(AbstractOperationState.class, OperationStateModificationMessage.class),
        WAVEFORM(RealTimeSampleArrayMetricState.class, WaveformStateModificationMessage.class);

        private Class<? extends AbstractState> changeBaseClass;
        private Class<? extends StateModificationMessage<?>> eventMessageClass;

        Type(Class<? extends AbstractState> changeBaseClass,
             Class<? extends StateModificationMessage<?>> eventMessageClass) {
            this.changeBaseClass = changeBaseClass;
            this.eventMessageClass = eventMessageClass;
        }

        Class<? extends AbstractState> getChangeBaseClass() {
            return changeBaseClass;
        }

        public Class<? extends StateModificationMessage<?>> getEventMessageClass() {
            return eventMessageClass;
        }
    }

    private MdibStateModifications(Type changeType) {
        this.changeType = changeType;
        this.states = new ArrayList<>();
    }

    private MdibStateModifications(Type changeType, int initialCapacity) {
        this.changeType = changeType;
        this.states = new ArrayList<>(initialCapacity);
    }
}
