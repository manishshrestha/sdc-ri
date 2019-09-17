package org.ieee11073.sdc.biceps.common;

import org.ieee11073.sdc.biceps.model.participant.*;
import org.ieee11073.sdc.biceps.testutil.MockModelFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class MdibTypeValidatorTest {
    private MdibTypeValidator matcher;

    @BeforeEach
    public void setUp() {
        matcher = new MdibTypeValidator();
    }

    @Test
    public void matchingClassTypes() {
        assertThat(matcher.match(AbstractContextDescriptor.class, PatientContextState.class), is(false));
        assertThat(matcher.match(LocationContextDescriptor.class, AbstractMetricState.class), is(false));
        assertThat(matcher.match(LocationContextDescriptor.class, PatientContextState.class), is(false));

        assertThat(matcher.match(NumericMetricDescriptor.class, NumericMetricState.class), is(true));
    }

    @Test
    public void matchingSinleStateInstance() throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        final StringMetricDescriptor descriptor = MockModelFactory.createDescriptor("handle", StringMetricDescriptor.class);
        final StringMetricState state = MockModelFactory.createState("handle", StringMetricState.class);
        final StringMetricState illegalSecondState = MockModelFactory.createState("handle", StringMetricState.class);
        final NumericMetricState mismatchingTypeState = MockModelFactory.createState("handle", NumericMetricState.class);
        final StringMetricState mismatchingHandleState = MockModelFactory.createState("invalid-handle", StringMetricState.class);

        assertThat(matcher.match(descriptor, state), is(true));
        assertThat(matcher.match(descriptor, Collections.singletonList(state)), is(true));

        assertThat(matcher.match(descriptor, Collections.singletonList(mismatchingTypeState)), is(false));
        assertThat(matcher.match(descriptor, Collections.singletonList(mismatchingHandleState)), is(false));
        assertThat(matcher.match(descriptor, Arrays.asList(state, illegalSecondState)), is(false));
        assertThat(matcher.match(descriptor, Collections.emptyList()), is(false));
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
