package org.somda.sdc.biceps.common;

import com.google.common.collect.ImmutableMap;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.biceps.testutil.MockModelFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MdibTypeValidatorTest {
    private MdibTypeValidator matcher;

    private static final Map<Class<? extends AbstractDescriptor>, Class<? extends AbstractState>> singleStateMap = ImmutableMap.<Class<? extends AbstractDescriptor>, Class<? extends AbstractState>>builder()
            .put(ActivateOperationDescriptor.class, ActivateOperationState.class)
            .put(AlertConditionDescriptor.class, AlertConditionState.class)
            .put(AlertSignalDescriptor.class, AlertSignalState.class)
            .put(AlertSystemDescriptor.class, AlertSystemState.class)
            .put(BatteryDescriptor.class, BatteryState.class)
            .put(ClockDescriptor.class, ClockState.class)
            .put(DistributionSampleArrayMetricDescriptor.class, DistributionSampleArrayMetricState.class)
            .put(EnumStringMetricDescriptor.class, EnumStringMetricState.class)
            .put(LimitAlertConditionDescriptor.class, LimitAlertConditionState.class)
            .put(MdsDescriptor.class, MdsState.class)
            .put(NumericMetricDescriptor.class, NumericMetricState.class)
            .put(RealTimeSampleArrayMetricDescriptor.class, RealTimeSampleArrayMetricState.class)
            .put(ScoDescriptor.class, ScoState.class)
            .put(SetAlertStateOperationDescriptor.class, SetAlertStateOperationState.class)
            .put(SetComponentStateOperationDescriptor.class, SetComponentStateOperationState.class)
            .put(SetContextStateOperationDescriptor.class, SetContextStateOperationState.class)
            .put(SetMetricStateOperationDescriptor.class, SetMetricStateOperationState.class)
            .put(SetStringOperationDescriptor.class, SetStringOperationState.class)
            .put(SetValueOperationDescriptor.class, SetValueOperationState.class)
            .put(StringMetricDescriptor.class, StringMetricState.class)
            .put(SystemContextDescriptor.class, SystemContextState.class)
            .put(VmdDescriptor.class, VmdState.class)
            .build();

    private static final Map<Class<? extends AbstractDescriptor>, Class<? extends AbstractMultiState>> multiStateMap = Map.of(
            EnsembleContextDescriptor.class, EnsembleContextState.class,
            LocationContextDescriptor.class, LocationContextState.class,
            MeansContextDescriptor.class, MeansContextState.class,
            OperatorContextDescriptor.class, OperatorContextState.class,
            PatientContextDescriptor.class, PatientContextState.class,
            WorkflowContextDescriptor.class, WorkflowContextState.class
    );

    @BeforeEach
    public void setUp() {
        matcher = new MdibTypeValidator();
    }

    @Test
    public void matchingClassTypes() {

        singleStateMap.forEach(
                (descOuter, stateOuter) -> {
                    singleStateMap.forEach(
                            (descInner, stateInner) -> {
                                if (descInner == descOuter) {
                                    assertTrue(matcher.match(descOuter, stateInner));
                                } else {
                                    assertFalse(matcher.match(descOuter, stateInner));
                                }
                            }
                    );
                    multiStateMap.forEach(
                            (descInner, stateInner) -> {
                                assertFalse(matcher.match(descOuter, stateInner));
                            }
                    );
                }
        );

        multiStateMap.forEach(
                (descOuter, stateOuter) -> {
                    multiStateMap.forEach(
                            (descInner, stateInner) -> {
                                if (descInner == descOuter) {
                                    assertTrue(matcher.match(descOuter, stateInner));
                                } else {
                                    assertFalse(matcher.match(descOuter, stateInner));
                                }
                            }
                    );
                    singleStateMap.forEach(
                            (descInner, stateInner) -> {
                                assertFalse(matcher.match(descOuter, stateInner));
                            }
                    );
                }
        );

    }

    @Test
    public <T extends AbstractDescriptor, U extends AbstractState> void matchSingleStateInstance() {

        singleStateMap.forEach(
                (descOuter, stateOuter) -> {
                    Class<T> descOuterCast = (Class<T>) descOuter; // to force one warning up here
                    Class<U> stateOuterCast = (Class<U>) stateOuter;
                    T descriptor = MockModelFactory.createDescriptor("handle", descOuterCast);
                    U state = MockModelFactory.createState("handle", stateOuterCast);
                    U illegalSecondState = MockModelFactory.createState("handle", stateOuterCast);

                    U mismatchingHandleState = MockModelFactory.createState("invalid-handle", stateOuterCast);

                    assertTrue(matcher.match(descriptor, state));
                    assertTrue(matcher.match(descriptor, Collections.singletonList(state)));

                    assertFalse(matcher.match(descriptor, mismatchingHandleState));
                    assertFalse(matcher.match(descriptor, Collections.singletonList(mismatchingHandleState)));
                    assertFalse(matcher.match(descriptor, Arrays.asList(state, illegalSecondState)));
                    assertFalse(matcher.match(descriptor, Collections.emptyList()));

                    singleStateMap.forEach(
                            (descInner, stateInner) -> {
                                if (descOuter == descInner) return; // don't compare with the same class
                                Class<U> stateInnerCast = (Class<U>) stateInner;
                                U illegalStateType = MockModelFactory.createState("handle", stateInnerCast);

                                assertFalse(matcher.match(descriptor, illegalStateType));
                                assertFalse(matcher.match(descriptor, Collections.singletonList(illegalStateType)));
                            }
                    );
                }
        );

    }

    @Test
    public void matchingMultiStateInstance() throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        final LocationContextDescriptor descriptor = MockModelFactory.createDescriptor("handle", LocationContextDescriptor.class);
        final List<LocationContextState> states = Arrays.asList(
                MockModelFactory.createContextState("c1", "handle", LocationContextState.class),
                MockModelFactory.createContextState("c2", "handle", LocationContextState.class),
                MockModelFactory.createContextState("c3", "handle", LocationContextState.class));

        final NumericMetricState mismatchingTypeState = MockModelFactory.createState("handle", NumericMetricState.class);
        final LocationContextState mismatchingHandleState = MockModelFactory.createContextState("c4", "invalid-handle", LocationContextState.class);

        assertThat(matcher.match(descriptor, states.get(0)), is(true));
        assertThat(matcher.match(descriptor, states), is(true));
        assertThat(matcher.match(descriptor, Collections.emptyList()), is(true));

        final List<LocationContextState> mismatchingHandleStates = new ArrayList<>(states);
        mismatchingHandleStates.add(mismatchingHandleState);
        assertThat(matcher.match(descriptor, mismatchingHandleStates), is(false));

        final List<AbstractState> mismatchingTypeStates = Arrays.asList(
                states.get(0),
                mismatchingTypeState,
                states.get(1));
        assertThat(matcher.match(descriptor, mismatchingTypeStates), is(false));
    }

    @Test
    public void singleAndMultiStateMatchers() {
        final List<AbstractDescriptor> singleStateDescriptors = Arrays.asList(
                new NumericMetricDescriptor(),
                new SetAlertStateOperationDescriptor(),
                new SystemContextDescriptor(),
                new MdsDescriptor());
        final List<AbstractDescriptor> multiStateDescriptors = Arrays.asList(
                new PatientContextDescriptor(),
                new LocationContextDescriptor(),
                new EnsembleContextDescriptor()
        );
        final List<AbstractState> singleStates = Arrays.asList(
                new NumericMetricState(),
                new MdsState(),
                new BatteryState(),
                new SetValueOperationState()
        );
        final List<AbstractState> multiStates = Arrays.asList(
                new LocationContextState(),
                new EnsembleContextState(),
                new WorkflowContextState()
        );

        singleStateDescriptors.stream().forEach(descriptor ->
                assertThat(matcher.isSingleStateDescriptor(descriptor), is(true)));

        multiStateDescriptors.stream().forEach(descriptor ->
                assertThat(matcher.isMultiStateDescriptor(descriptor), is(true)));

        singleStates.stream().forEach(state ->
                assertThat(matcher.isSingleState(state), is(true)));

        multiStates.stream().forEach(state ->
                assertThat(matcher.isMultiState(state), is(true)));
    }
}
