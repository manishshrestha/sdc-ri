package org.ieee11073.sdc.biceps.common;

import org.ieee11073.sdc.biceps.common.event.*;
import org.ieee11073.sdc.biceps.model.participant.*;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Container to collect state updates supposed to be applied on an MDIB.
 *
 * The {@linkplain MdibStateModifications} is a fluent interface.
 */
public class MdibStateModifications {
    private final MdibStateModifications.Type changeType;
    private final MdibVersion mdibVersion;
    private List<AbstractState> states;

    /**
     * Create set.
     */
    public static MdibStateModifications create(Type changeType) {
        return new MdibStateModifications(changeType, null);
    }

    /**
     * Create set with version number.
     */
    public static MdibStateModifications create(Type changeType, MdibVersion mdibVersion) {
        return new MdibStateModifications(changeType, mdibVersion);
    }

    /**
     * Create set with version number form existing base.
     */
    public static MdibStateModifications create(MdibVersion mdibVersion, MdibStateModifications existingModifications) {
        MdibStateModifications newModifications = new MdibStateModifications(existingModifications.getChangeType(), mdibVersion);
        newModifications.states = existingModifications.states;
        return newModifications;
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
    List<AbstractState> getStates() {
        return this.states;
    }

    public Type getChangeType() {
        return changeType;
    }

    public Optional<MdibVersion> getMdibVersion() {
        return Optional.ofNullable(mdibVersion);
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
        ALERT(AbstractAlertState.class, AlertStateModificationMessage.class),
        COMPONENT(AbstractDeviceComponentState.class, ComponentStateModificationMessage.class),
        CONTEXT(AbstractContextState.class, ContextStateModificationMessage.class),
        METRIC(AbstractMetricState.class, MetricStateModificationMessage.class),
        OPERATION(AbstractOperationState.class, OperationStateModificationMessage.class);

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

    private MdibStateModifications(Type changeType, @Nullable MdibVersion mdibVersion) {
        this.changeType = changeType;
        this.mdibVersion = mdibVersion;
        this.states = new ArrayList<>();
    }
}
