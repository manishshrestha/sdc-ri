package org.ieee11073.sdc.biceps.common;

import com.google.inject.Injector;
import org.ieee11073.sdc.biceps.UnitTestUtil;
import org.ieee11073.sdc.biceps.common.factory.MdibEntityFactory;
import org.ieee11073.sdc.biceps.model.participant.MdibVersion;
import org.ieee11073.sdc.biceps.model.participant.PatientContextDescriptor;
import org.ieee11073.sdc.biceps.model.participant.PatientContextState;
import org.ieee11073.sdc.biceps.testutil.MockModelFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MdibEntityImplTest {
    private static final UnitTestUtil UT = new UnitTestUtil();

    private MdibEntityFactory mdibEntityFactory;
    private String expectedDescriptorHandle;
    private List<String> expectedStateHandles;
    private MdibEntity mdibEntity;

    @BeforeEach
    public void setUp() throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Injector injector = UT.getInjector();
        mdibEntityFactory = injector.getInstance(MdibEntityFactory.class);
        expectedDescriptorHandle = "descrHandle";
        expectedStateHandles = Arrays.asList("stateHandle1", "stateHandle2", "stateHandle3");
        mdibEntity = mdibEntityFactory.createMdibEntity(
                null,
                Collections.emptyList(),
                MockModelFactory.createDescriptor(expectedDescriptorHandle, PatientContextDescriptor.class),
                expectedStateHandles.stream()
                        .map(handle -> {
                            try {
                                return MockModelFactory.createContextState(handle, expectedDescriptorHandle, PatientContextState.class);
                            } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
                                throw new RuntimeException("Creation of context state failed. Handle: " + handle);
                            }
                        })
                        .collect(Collectors.toList()),
                MdibVersion.create());
    }

    @Test
    public void mdibEntityGetters() {
        assertThat(mdibEntity.getParent(), is(Optional.empty()));
        assertThat(mdibEntity.getChildren().isEmpty(), is(true));
        assertThat(mdibEntity.getDescriptor(), instanceOf(PatientContextDescriptor.class));
        assertThat(mdibEntity.getDescriptor().getHandle(), is(expectedDescriptorHandle));
        assertThat(mdibEntity.getStates().size(), is(expectedStateHandles.size()));
        for (int i = 0; i < expectedStateHandles.size(); ++i) {
            assertThat(mdibEntity.getStates().get(i), instanceOf(PatientContextState.class));
            PatientContextState state = (PatientContextState) mdibEntity.getStates().get(i);
            assertThat(state.getHandle(), is(expectedStateHandles.get(i)));
            assertThat(state.getDescriptorHandle(), is(expectedDescriptorHandle));
        }
    }
}
