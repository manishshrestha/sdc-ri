package org.somda.sdc.glue.common;

import org.somda.sdc.biceps.model.participant.AbstractAlertState;
import org.somda.sdc.biceps.model.participant.AbstractOperationState;
import org.somda.sdc.biceps.model.participant.AlertActivation;
import org.somda.sdc.biceps.model.participant.AlertConditionMonitoredLimits;
import org.somda.sdc.biceps.model.participant.ClockState;
import org.somda.sdc.biceps.model.participant.LimitAlertConditionState;
import org.somda.sdc.biceps.model.participant.OperatingMode;
import org.somda.sdc.biceps.model.participant.Range;

/**
 * Defines required default values for all states.
 * <p>
 * Derive from this class to define more defaults. Required elements/attributes and their default values:
 * <ul>
 * <li>{@link AbstractAlertState#setActivationState(AlertActivation)} is set to {@link AlertActivation#ON}
 * <li>{@link LimitAlertConditionState#setLimits(Range)} is set to empty {@link Range}
 * <li>{@link LimitAlertConditionState#setMonitoredAlertLimits(AlertConditionMonitoredLimits)}
 * is set to {@link AlertConditionMonitoredLimits#ALL}
 * <li>{@link AbstractOperationState#setOperatingMode(OperatingMode)} is set to {@link OperatingMode#EN}
 * <li>{@link ClockState#setRemoteSync(boolean)} is set to false
 * </ul>
 */
public class RequiredDefaultStateValues implements DefaultStateValues {

    AbstractAlertState onAbstractAlertState(AbstractAlertState state) {
        return state.newCopyBuilder()
            .withActivationState(AlertActivation.ON)
            .build();
    }

    LimitAlertConditionState onLimitAlertConditionState(LimitAlertConditionState state) {
        var aState = (LimitAlertConditionState) onAbstractAlertState(state);
        return aState.newCopyBuilder()
            .withLimits(new Range())
            .withMonitoredAlertLimits(AlertConditionMonitoredLimits.ALL)
            .build();
    }

    AbstractOperationState onAbstractOperationState(AbstractOperationState state) {
        return state.newCopyBuilder().withOperatingMode(OperatingMode.EN).build();
    }

    ClockState onClockState(ClockState state) {
        return state.newCopyBuilder().withRemoteSync(false).build();
    }

}
