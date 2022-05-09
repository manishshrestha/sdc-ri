package org.somda.sdc.biceps.common;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.biceps.testutil.MockModelFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.org.somda.common.LoggingTestWatcher;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(LoggingTestWatcher.class)
class MdibTypeValidatorTest {
    private MdibTypeValidator matcher;

    private static final Map<? extends AbstractDescriptor.Builder<?>, ? extends AbstractState.Builder<?>> singleStateMap
            // Map.of is only defined up to 10 elements, so we need to use the builder
        = ImmutableMap.<AbstractDescriptor.Builder<?>, AbstractState.Builder<?>>builder()
            .put(ActivateOperationDescriptor.builder(), ActivateOperationState.builder())
            .put(AlertConditionDescriptor.builder(), AlertConditionState.builder())
            .put(AlertSignalDescriptor.builder(), AlertSignalState.builder())
            .put(AlertSystemDescriptor.builder(), AlertSystemState.builder())
            .put(BatteryDescriptor.builder(), BatteryState.builder())
            .put(ClockDescriptor.builder(), ClockState.builder())
            .put(DistributionSampleArrayMetricDescriptor.builder(), DistributionSampleArrayMetricState.builder())
            .put(EnumStringMetricDescriptor.builder(), EnumStringMetricState.builder())
            .put(LimitAlertConditionDescriptor.builder(), LimitAlertConditionState.builder())
            .put(MdsDescriptor.builder(), MdsState.builder())
            .put(NumericMetricDescriptor.builder(), NumericMetricState.builder())
            .put(RealTimeSampleArrayMetricDescriptor.builder(), RealTimeSampleArrayMetricState.builder())
            .put(ScoDescriptor.builder(), ScoState.builder())
            .put(SetAlertStateOperationDescriptor.builder(), SetAlertStateOperationState.builder())
            .put(SetComponentStateOperationDescriptor.builder(), SetComponentStateOperationState.builder())
            .put(SetContextStateOperationDescriptor.builder(), SetContextStateOperationState.builder())
            .put(SetMetricStateOperationDescriptor.builder(), SetMetricStateOperationState.builder())
            .put(SetStringOperationDescriptor.builder(), SetStringOperationState.builder())
            .put(SetValueOperationDescriptor.builder(), SetValueOperationState.builder())
            .put(StringMetricDescriptor.builder(), StringMetricState.builder())
            .put(SystemContextDescriptor.builder(), SystemContextState.builder())
            .put(VmdDescriptor.builder(), VmdState.builder())
            .build();

    private static final Map<? extends AbstractDescriptor.Builder<?>, ? extends AbstractContextState.Builder<?>> multiStateMap = Map.of(
            EnsembleContextDescriptor.builder(), EnsembleContextState.builder(),
            LocationContextDescriptor.builder(), LocationContextState.builder(),
            MeansContextDescriptor.builder(), MeansContextState.builder(),
            OperatorContextDescriptor.builder(), OperatorContextState.builder(),
            PatientContextDescriptor.builder(), PatientContextState.builder(),
            WorkflowContextDescriptor.builder(), WorkflowContextState.builder()
    );

    @BeforeEach
    public void setUp() {
        matcher = new MdibTypeValidator();
    }

    @Test
    void matchingClassTypes() {

        singleStateMap.forEach(
                (descOuter, stateOuter) -> {
                    var descOuterClass = MockModelFactory.createDescriptor("handle", descOuter).build().getClass();

                    singleStateMap.forEach(
                            (descInner, stateInner) -> {
                                var stateInnerClass = MockModelFactory.createState("handle", stateInner).build().getClass();

                                if (descInner == descOuter) {
                                    assertTrue(matcher.match(descOuterClass, stateInnerClass));
                                } else {
                                    assertFalse(matcher.match(descOuterClass, stateInnerClass));
                                }
                            }
                    );
                    multiStateMap.forEach(
                            (descInner, stateInner) -> {
                                var stateInnerClass = MockModelFactory.createState("handle", stateInner).build().getClass();
                                assertFalse(matcher.match(descOuterClass, stateInnerClass));
                            }
                    );
                }
        );

        multiStateMap.forEach(
                (descOuter, stateOuter) -> {
                    var descOuterClass = MockModelFactory.createDescriptor("handle", descOuter).build().getClass();
                    multiStateMap.forEach(
                            (descInner, stateInner) -> {
                                var stateInnerClass = MockModelFactory.createState("handle", stateInner).build().getClass();

                                if (descInner == descOuter) {
                                    assertTrue(matcher.match(descOuterClass, stateInnerClass));
                                } else {
                                    assertFalse(matcher.match(descOuterClass, stateInnerClass));
                                }
                            }
                    );
                    singleStateMap.forEach(
                            (descInner, stateInner) -> {
                                var stateInnerClass = MockModelFactory.createState("handle", stateInner).build().getClass();

                                assertFalse(matcher.match(descOuterClass, stateInnerClass));
                            }
                    );
                }
        );

    }

    @Test
    void matchSingleStateInstance() {
        singleStateMap.forEach(
                (descOuter, stateOuter) -> {
                    var descriptor = MockModelFactory.createDescriptor("handle", descOuter).build();
                    var state = MockModelFactory.createState("handle", stateOuter).build();
                    var illegalSecondState = MockModelFactory.createState("handle", stateOuter).build();
                    var mismatchingHandleState = MockModelFactory.createState("invalid-handle", stateOuter).build();

                    assertTrue(matcher.match(descriptor, state));
                    assertTrue(matcher.match(descriptor, Collections.singletonList(state)));

                    assertFalse(matcher.match(descriptor, mismatchingHandleState));
                    assertFalse(matcher.match(descriptor, Collections.singletonList(mismatchingHandleState)));
                    assertFalse(matcher.match(descriptor, Arrays.asList(state, illegalSecondState)));
                    assertFalse(matcher.match(descriptor, Collections.emptyList()));

                    singleStateMap.forEach(
                            (descInner, stateInner) -> {
                                if (descOuter == descInner) return; // don't compare with the same class
                                var illegalStateType = MockModelFactory.createState("handle", stateInner).build();

                                assertFalse(matcher.match(descriptor, illegalStateType));
                                assertFalse(matcher.match(descriptor, Collections.singletonList(illegalStateType)));
                            }
                    );

                }
        );
    }

    @Test
    void matchingMultiStateInstance() {
        multiStateMap.forEach(
                (descOuter, stateOuter) -> {
                    var descriptor = MockModelFactory.createDescriptor("handle", descOuter).build();
                    var states = Arrays.asList(
                            MockModelFactory.createContextState("c1", "handle", stateOuter).build(),
                            MockModelFactory.createContextState("c2", "handle", stateOuter).build(),
                            MockModelFactory.createContextState("c3", "handle", stateOuter).build()
                    );

                    var mismatchingHandleState = MockModelFactory.createContextState("c4", "invalid-handle", stateOuter).build();
                    final var mismatchingHandleStates = new ArrayList<>(states);
                    mismatchingHandleStates.add(mismatchingHandleState);

                    assertTrue(matcher.match(descriptor, states.get(0)));
                    assertTrue(matcher.match(descriptor, states));
                    assertTrue(matcher.match(descriptor, Collections.emptyList()));

                    assertFalse(matcher.match(descriptor, mismatchingHandleStates));

                    multiStateMap.forEach(
                            (descInner, stateInner) -> {
                                if (descOuter == descInner) return; // don't compare with the same class
                                var mismatchingTypeState = MockModelFactory.createContextState("e1", "handle", stateInner).build();

                                final List<AbstractState> mismatchingTypeStates = Arrays.asList(
                                        states.get(0),
                                        mismatchingTypeState,
                                        states.get(1));
                                assertFalse(matcher.match(descriptor, mismatchingTypeStates));
                            }
                    );

                    singleStateMap.forEach(
                            (descInner, stateInner) -> {
                                if (descOuter == descInner) return; // don't compare with the same class
                                var illegalStateType = MockModelFactory.createState("handle", stateInner).build();

                                assertFalse(matcher.match(descriptor, illegalStateType));
                                final List<AbstractState> mismatchingTypeStates = Arrays.asList(
                                        states.get(0),
                                        illegalStateType,
                                        states.get(1));
                                assertFalse(matcher.match(descriptor, Collections.singletonList(illegalStateType)));
                            }
                    );

                }
        );
    }

    @Test
    void singleAndMultiStateMatchers() {
        singleStateMap.forEach(
                (descOuter, stateOuter) -> {
                    var descriptor = MockModelFactory.createDescriptor("handle", descOuter).build();
                    var state = MockModelFactory.createState("handle", stateOuter).build();

                    assertTrue(matcher.isSingleStateDescriptor(descriptor));
                    assertTrue(matcher.isSingleState(state));
                }
        );

        multiStateMap.forEach(
                (descOuter, stateOuter) -> {
                    var descriptor = MockModelFactory.createDescriptor("handle", descOuter).build();
                    var state = MockModelFactory.createContextState("c", "handle", stateOuter).build();

                    assertTrue(matcher.isMultiStateDescriptor(descriptor));
                    assertTrue(matcher.isMultiState(state));
                }
        );
    }

}
