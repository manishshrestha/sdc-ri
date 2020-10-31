package org.somda.sdc.proto.mapping.participant;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.participant.AlertActivation;
import org.somda.sdc.biceps.model.participant.AlertConditionKind;
import org.somda.sdc.biceps.model.participant.AlertConditionMonitoredLimits;
import org.somda.sdc.biceps.model.participant.AlertConditionPriority;
import org.somda.sdc.biceps.model.participant.LimitAlertConditionDescriptor;
import org.somda.sdc.biceps.model.participant.LimitAlertConditionState;
import org.somda.sdc.biceps.model.participant.Range;
import org.somda.sdc.biceps.model.participant.SafetyClassification;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.testutil.Handles;
import org.somda.sdc.proto.UnitTestUtil;
import org.somda.sdc.proto.mapping.TypeCollection;

import java.math.BigInteger;
import java.time.Duration;
import java.util.List;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LimitAlertConditionRoundTrip implements BiConsumer<LocalMdibAccess, RemoteMdibAccess> {

    private static final String HANDLE = Handles.ALERTCONDITION_1;
    private static final String HANDLE_MIN = HANDLE + "Min";

    LimitAlertConditionRoundTrip(MdibDescriptionModifications modifications) {
        bigSet(modifications);
        minimalSet(modifications);
    }


    void bigSet(MdibDescriptionModifications modifications) {
        var descriptor = new LimitAlertConditionDescriptor();
        {
            descriptor.setHandle(HANDLE);
            descriptor.setDescriptorVersion(BigInteger.valueOf(123123));
            descriptor.setSafetyClassification(SafetyClassification.MED_C);
            descriptor.setKind(AlertConditionKind.PHY);
            descriptor.setPriority(AlertConditionPriority.HI);
            descriptor.setDefaultConditionGenerationDelay(Duration.ofMillis(33));
            descriptor.setCanEscalate(AlertConditionPriority.LO);
            descriptor.setCanDeescalate(AlertConditionPriority.LO);

            descriptor.setType(TypeCollection.CODED_VALUE);
            descriptor.setSource(List.of("Ṯ̤͍̥͇͈h̲́e͏͓̼̗̙̼̣͔ ͇", "̜̱̠͓͍ͅN͕͠e̗̱z̘̝̜̺͙p̤̺̹͍̯͚e̠̻̠͜r̨̤͍̺̖͔̖̖d̠̟̭̬̝͟i̦͖̩͓͔̤a̠̗̬͉̙n͚͜ ", "̻̞̰͚ͅh̵͉i̳̞v̢͇ḙ͎͟-҉̭̩̼͔m̤̭̫i͕͇̝̦n̗͙ḍ̟"));
            descriptor.setCauseInfo(List.of(TypeCollection.CAUSE_INFO));

            descriptor.setAutoLimitSupported(false);
            descriptor.setMaxLimits(new Range());

            // TODO: Extension
//            descriptor.setExtension();
        }
        var state = new LimitAlertConditionState();
        {
            state.setStateVersion(BigInteger.TEN);
            state.setDescriptorHandle(HANDLE);
            state.setDescriptorVersion(descriptor.getDescriptorVersion());
            state.setActivationState(AlertActivation.ON);
            state.setActualConditionGenerationDelay(Duration.ofDays(4));
            state.setActualPriority(AlertConditionPriority.LO);
            state.setRank(1337);
            state.setPresence(false);
            state.setDeterminationTime(UnitTestUtil.makeTestTimestamp());

            state.setMonitoredAlertLimits(AlertConditionMonitoredLimits.ALL);
            state.setAutoLimitActivationState(AlertActivation.OFF);
            state.setLimits(TypeCollection.RANGE);

            // TODO: Extension
//            state.setExtension();
        }
        modifications.insert(descriptor, state, Handles.ALERTSYSTEM_1);
    }

    void minimalSet(MdibDescriptionModifications modifications) {
        var descriptor = new LimitAlertConditionDescriptor();
        {
            descriptor.setHandle(HANDLE_MIN);
            descriptor.setKind(AlertConditionKind.PHY);
            descriptor.setPriority(AlertConditionPriority.HI);
            descriptor.setMaxLimits(new Range());

        }
        var state = new LimitAlertConditionState();
        {
            state.setDescriptorHandle(HANDLE_MIN);
            state.setActivationState(AlertActivation.ON);
            state.setMonitoredAlertLimits(AlertConditionMonitoredLimits.ALL);
            state.setLimits(new Range());
        }
        modifications.insert(descriptor, state, Handles.ALERTSYSTEM_0);
    }

    @Override
    public void accept(final LocalMdibAccess localMdibAccess, final RemoteMdibAccess remoteMdibAccess) {
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE, LimitAlertConditionDescriptor.class).get();
            var expectedState = localMdibAccess.getState(HANDLE, LimitAlertConditionState.class).get();
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE, LimitAlertConditionDescriptor.class).get();
            var actualState = remoteMdibAccess.getState(HANDLE, LimitAlertConditionState.class).get();

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE_MIN, LimitAlertConditionDescriptor.class).get();
            var expectedState = localMdibAccess.getState(HANDLE_MIN, LimitAlertConditionState.class).get();
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE_MIN, LimitAlertConditionDescriptor.class).get();
            var actualState = remoteMdibAccess.getState(HANDLE_MIN, LimitAlertConditionState.class).get();

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
    }
}
