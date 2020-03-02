package org.somda.sdc.biceps.common.storage;

import com.google.inject.Injector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.somda.sdc.biceps.UnitTestUtil;
import org.somda.sdc.biceps.common.MdibDescriptionModification;
import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.common.MdibStateModifications;
import org.somda.sdc.biceps.common.storage.factory.MdibStorageFactory;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.biceps.testutil.Handles;
import org.somda.sdc.biceps.testutil.MockModelFactory;
import test.org.somda.common.TestLogging;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class MdibStorageImplTest {
    private static final UnitTestUtil UT = new UnitTestUtil();

    private MdibStorage mdibStorage;

    @BeforeEach
    void setUp() {
        TestLogging.configure();
        Injector injector = UT.getInjector();
        mdibStorage = injector.getInstance(MdibStorageFactory.class).createMdibStorage();
    }

    private void applyDescriptionWithVersion(MdibDescriptionModification.Type type,
                                             BigInteger version) {
        final MdibDescriptionModifications modifications = MdibDescriptionModifications.create();
        modifications.add(type,
                MockModelFactory.createDescriptor(Handles.MDS_0, version, MdsDescriptor.class),
                MockModelFactory.createState(Handles.MDS_0, version, MdsState.class));
        modifications.add(type,
                MockModelFactory.createDescriptor(Handles.SYSTEMCONTEXT_0, version, MdsDescriptor.class),
                MockModelFactory.createState(Handles.SYSTEMCONTEXT_0, version, SystemContextState.class),
                Handles.MDS_0);
        modifications.add(type,
                MockModelFactory.createDescriptor(Handles.VMD_0, version, VmdDescriptor.class),
                MockModelFactory.createState(Handles.VMD_0, version, VmdState.class),
                Handles.MDS_0);
        modifications.add(type,
                MockModelFactory.createDescriptor(Handles.CHANNEL_0, version, ChannelDescriptor.class),
                MockModelFactory.createState(Handles.CHANNEL_0, version, ChannelState.class),
                Handles.VMD_0);
        modifications.add(type,
                MockModelFactory.createDescriptor(Handles.CHANNEL_1, version, ChannelDescriptor.class),
                MockModelFactory.createState(Handles.CHANNEL_1, version, ChannelState.class),
                Handles.VMD_0);
        modifications.add(type,
                MockModelFactory.createDescriptor(Handles.CONTEXTDESCRIPTOR_0,
                        version,
                        PatientContextDescriptor.class),
                Arrays.asList(
                        MockModelFactory.createContextState(Handles.CONTEXT_0, Handles.CONTEXTDESCRIPTOR_0,
                                version, version, PatientContextState.class),
                        MockModelFactory.createContextState(Handles.CONTEXT_1, Handles.CONTEXTDESCRIPTOR_0,
                                version, version, PatientContextState.class)
                ),
                Handles.SYSTEMCONTEXT_0);

        modifications.add(type,
                MockModelFactory.createDescriptor(Handles.CONTEXTDESCRIPTOR_1,
                        version,
                        LocationContextDescriptor.class),
                Arrays.asList(
                        MockModelFactory.createContextState(Handles.CONTEXT_2, Handles.CONTEXTDESCRIPTOR_1,
                                version, version, LocationContextState.class)
                ),
                Handles.SYSTEMCONTEXT_0);

        mdibStorage.apply(mock(MdibVersion.class), mock(BigInteger.class), mock(BigInteger.class), modifications);
    }

    private void testWithVersion(List<String> testedHandles, BigInteger version) {
        testWithVersion(testedHandles, version, version);
    }

    private void testWithVersion(List<String> testedHandles, BigInteger descrVersion, BigInteger stateVersion) {
        testedHandles.forEach(handle -> {
            assertThat(mdibStorage.getEntity(handle).isPresent(), is(true));
            assertThat(mdibStorage.getEntity(handle).get().getDescriptor().getDescriptorVersion(), is(descrVersion));
            mdibStorage.getEntity(handle).get().getStates().forEach(state ->
                    assertThat(state.getStateVersion(), is(stateVersion)));
        });
    }

    @Test
    void writeDescription() {
        List<String> testedHandles = Arrays.asList(
                Handles.MDS_0,
                Handles.SYSTEMCONTEXT_0,
                Handles.VMD_0,
                Handles.CHANNEL_0,
                Handles.CHANNEL_1,
                Handles.CONTEXTDESCRIPTOR_0);

        applyDescriptionWithVersion(MdibDescriptionModification.Type.INSERT, BigInteger.ZERO);
        testWithVersion(testedHandles, BigInteger.ZERO);
        applyDescriptionWithVersion(MdibDescriptionModification.Type.UPDATE, BigInteger.ONE);
        testWithVersion(testedHandles, BigInteger.ONE);
        applyDescriptionWithVersion(MdibDescriptionModification.Type.UPDATE, BigInteger.TEN);
        testWithVersion(testedHandles, BigInteger.TEN);

        applyDescriptionWithVersion(MdibDescriptionModification.Type.DELETE, BigInteger.ZERO);
        testedHandles.forEach(handle -> {
            assertThat(mdibStorage.getEntity(handle).isPresent(), is(false));
        });
    }

    @Test
    void mdibAccess() {
        applyDescriptionWithVersion(MdibDescriptionModification.Type.INSERT, BigInteger.ZERO);
        assertThat(mdibStorage.getEntity(Handles.UNKNOWN).isPresent(), is(false));
        assertThat(mdibStorage.getEntity(Handles.MDS_0).isPresent(), is(true));

        assertThat(mdibStorage.getDescriptor(Handles.UNKNOWN, MdsDescriptor.class).isPresent(), is(false));
        assertThat(mdibStorage.getDescriptor(Handles.VMD_0, MdsDescriptor.class).isPresent(), is(false));
        assertThat(mdibStorage.getDescriptor(Handles.VMD_0, VmdDescriptor.class).isPresent(), is(true));

        assertThat(mdibStorage.getContextStates(Handles.UNKNOWN, PatientContextState.class).isEmpty(), is(true));
        assertThat(mdibStorage.getContextStates(Handles.VMD_0, PatientContextState.class).isEmpty(), is(true));
        assertThat(mdibStorage.getContextStates(Handles.CONTEXTDESCRIPTOR_0, LocationContextState.class).isEmpty(), is(true));
        assertThat(mdibStorage.getContextStates(Handles.CONTEXTDESCRIPTOR_0, PatientContextState.class).size(), is(2));

        assertThat(mdibStorage.findContextStatesByType(PatientContextState.class).size(), is(2));
        assertThat(mdibStorage.findContextStatesByType(LocationContextState.class).size(), is(1));
    }

    @Test
    void writeStates() {
        List<String> testedHandles = Arrays.asList(
                Handles.ALERTSYSTEM_0,
                Handles.ALERTCONDITION_0,
                Handles.ALERTCONDITION_1);

        final MdibDescriptionModifications descriptionModifications = MdibDescriptionModifications.create();
        descriptionModifications.insert(
                MockModelFactory.createDescriptor(Handles.MDS_0, MdsDescriptor.class),
                MockModelFactory.createState(Handles.MDS_0, MdsState.class));
        descriptionModifications.insert(
                MockModelFactory.createDescriptor(Handles.ALERTSYSTEM_0, AlertSystemDescriptor.class),
                MockModelFactory.createState(Handles.ALERTSYSTEM_0, AlertSystemState.class),
                Handles.MDS_0);
        descriptionModifications.insert(
                MockModelFactory.createDescriptor(Handles.ALERTCONDITION_0, AlertConditionDescriptor.class),
                MockModelFactory.createState(Handles.ALERTCONDITION_0, AlertConditionState.class),
                Handles.ALERTSYSTEM_0);
        descriptionModifications.insert(
                MockModelFactory.createDescriptor(Handles.ALERTCONDITION_1, AlertConditionDescriptor.class),
                MockModelFactory.createState(Handles.ALERTCONDITION_1, AlertConditionState.class),
                Handles.ALERTSYSTEM_0);
        descriptionModifications.insert(
                MockModelFactory.createDescriptor(Handles.SYSTEMCONTEXT_0, SystemContextDescriptor.class),
                MockModelFactory.createState(Handles.SYSTEMCONTEXT_0, SystemContextState.class));
        descriptionModifications.insert(
                MockModelFactory.createDescriptor(Handles.CONTEXTDESCRIPTOR_0, PatientContextDescriptor.class),
                Arrays.asList(
                        MockModelFactory.createContextState(Handles.CONTEXT_0, Handles.CONTEXTDESCRIPTOR_0, PatientContextState.class),
                        MockModelFactory.createContextState(Handles.CONTEXT_1, Handles.CONTEXTDESCRIPTOR_0, PatientContextState.class)));
        descriptionModifications.insert(
                MockModelFactory.createDescriptor(Handles.CONTEXTDESCRIPTOR_1, LocationContextDescriptor.class)
        );

        mdibStorage.apply(mock(MdibVersion.class), mock(BigInteger.class), mock(BigInteger.class), descriptionModifications);

        testWithVersion(testedHandles, BigInteger.ZERO);

        MdibStateModifications stateModifications = MdibStateModifications.create(MdibStateModifications.Type.ALERT);
        stateModifications.add(MockModelFactory.createState(Handles.ALERTSYSTEM_0, BigInteger.ONE, AlertSystemState.class));
        stateModifications.add(MockModelFactory.createState(Handles.ALERTCONDITION_0, BigInteger.ONE, AlertConditionState.class));
        stateModifications.add(MockModelFactory.createState(Handles.ALERTCONDITION_1, BigInteger.ONE, AlertConditionState.class));
        try {
            stateModifications.add(MockModelFactory.createState(Handles.MDS_0, BigInteger.ONE, MdsState.class));
            Assertions.fail("Could add MDS to alert state change set");
        } catch (Exception ignored) {
        }

        mdibStorage.apply(mock(MdibVersion.class), mock(BigInteger.class), stateModifications);
        testWithVersion(testedHandles, BigInteger.ZERO, BigInteger.ONE);

        testedHandles = Arrays.asList(Handles.CONTEXTDESCRIPTOR_0);

        stateModifications = MdibStateModifications.create(MdibStateModifications.Type.CONTEXT);

        stateModifications.add(MockModelFactory.createContextState(Handles.CONTEXT_0, Handles.CONTEXTDESCRIPTOR_0, BigInteger.ONE, PatientContextState.class));
        stateModifications.add(MockModelFactory.createContextState(Handles.CONTEXT_1, Handles.CONTEXTDESCRIPTOR_0, BigInteger.ONE, PatientContextState.class));
        mdibStorage.apply(mock(MdibVersion.class), mock(BigInteger.class), stateModifications);

        testWithVersion(testedHandles, BigInteger.ZERO, BigInteger.ONE);

        // add a MultiState to a descriptor without any previous states
        stateModifications = MdibStateModifications.create(MdibStateModifications.Type.CONTEXT);
        stateModifications.add(MockModelFactory.createContextState(Handles.CONTEXT_2, Handles.CONTEXTDESCRIPTOR_1, BigInteger.ONE, LocationContextState.class));
        mdibStorage.apply(mock(MdibVersion.class), mock(BigInteger.class), stateModifications);
        assertThat(mdibStorage.getContextStates(Handles.CONTEXTDESCRIPTOR_1, LocationContextState.class).size(), is(1));

        // add a second MultiState
        stateModifications = MdibStateModifications.create(MdibStateModifications.Type.CONTEXT);
        stateModifications.add(MockModelFactory.createContextState(Handles.CONTEXT_3, Handles.CONTEXTDESCRIPTOR_1, BigInteger.ONE, LocationContextState.class));
        mdibStorage.apply(mock(MdibVersion.class), mock(BigInteger.class), stateModifications);
        assertThat(mdibStorage.getContextStates(Handles.CONTEXTDESCRIPTOR_1, LocationContextState.class).size(), is(2));
    }

    @Test
    void writeNotAssociatedContextStatesThroughDescriptionUpdate() {
        applyDescriptionWithVersion(MdibDescriptionModification.Type.INSERT, BigInteger.ZERO);

        // Make sure there are two states in the MDIB for tested patient context
        var states = mdibStorage.findContextStatesByType(PatientContextState.class);
        assertEquals(2, states.size());

        // Get states and set the first one not-associated
        var entity = mdibStorage.getEntity(Handles.CONTEXTDESCRIPTOR_0);
        assertTrue(entity.isPresent());
        states = entity.get().getStates(PatientContextState.class);
        assertEquals(2, states.size());
        states.get(0).setContextAssociation(ContextAssociation.NO);
        var modifications = MdibDescriptionModifications.create()
                .update(entity.get().getDescriptor(), states);
        var result = mdibStorage.apply(mock(MdibVersion.class), mock(BigInteger.class), mock(BigInteger.class), modifications);

        // After write check that there was one updated entity with two updated states where one state is not associated
        assertEquals(1, result.getUpdatedEntities().size());
        assertEquals(2, result.getUpdatedEntities().get(0).getStates().size());
        states = result.getUpdatedEntities().get(0).getStates(PatientContextState.class);
        assertEquals(Handles.CONTEXT_0, states.get(0).getHandle());
        assertEquals(ContextAssociation.NO, states.get(0).getContextAssociation());

        // Check that the state is missing from the MDIB
        entity = mdibStorage.getEntity(Handles.CONTEXTDESCRIPTOR_0);
        assertTrue(entity.isPresent());
        states = entity.get().getStates(PatientContextState.class);
        assertEquals(1, states.size());
        assertEquals(Handles.CONTEXT_1, states.get(0).getHandle());

        // Whitebox: triggers internal access to contextStates map
        // That one shall be updated, too
        states = mdibStorage.findContextStatesByType(PatientContextState.class);
        assertEquals(1, states.size());
    }

    @Test
    void writeNotAssociatedContextStatesThroughDescriptionInsert() {
        applyDescriptionWithVersion(MdibDescriptionModification.Type.INSERT, BigInteger.ZERO);

        // Make sure there are two states in the MDIB for tested patient context
        var states = mdibStorage.findContextStatesByType(EnsembleContextState.class);
        assertEquals(0, states.size());

        // Get states and add another one as not associated
        var newDescriptor = new EnsembleContextDescriptor();
        newDescriptor.setHandle(Handles.CONTEXTDESCRIPTOR_2);
        var newState = new EnsembleContextState();
        newState.setDescriptorHandle(Handles.CONTEXTDESCRIPTOR_2);
        newState.setHandle(Handles.CONTEXT_3);
        newState.setContextAssociation(ContextAssociation.NO);
        states.add(newState);
        var modifications = MdibDescriptionModifications.create()
                .insert(newDescriptor, states);
        var result = mdibStorage.apply(mock(MdibVersion.class), mock(BigInteger.class), mock(BigInteger.class), modifications);

        // After write check that there was one inserted entity with one inserted state where that one state is not associated
        assertEquals(1, result.getInsertedEntities().size());
        assertEquals(1, result.getInsertedEntities().get(0).getStates().size());
        states = result.getInsertedEntities().get(0).getStates(EnsembleContextState.class);
        assertEquals(Handles.CONTEXT_3, states.get(0).getHandle());
        assertEquals(ContextAssociation.NO, states.get(0).getContextAssociation());

        // Check that the state is missing from the MDIB as a not-associated context state is never written to the MDIB
        var entity = mdibStorage.getEntity(Handles.CONTEXTDESCRIPTOR_2);
        assertTrue(entity.isPresent());
        states = entity.get().getStates(EnsembleContextState.class);
        assertEquals(0, states.size());

        // Whitebox: triggers internal access to contextStates map
        states = mdibStorage.findContextStatesByType(EnsembleContextState.class);
        assertEquals(0, states.size());
    }

    @Test
    void writeNotAssociatedContextStatesThroughStateUpdate() {
        applyDescriptionWithVersion(MdibDescriptionModification.Type.INSERT, BigInteger.ZERO);

        {
            // Read disassociated context, write to associated
            var state = mdibStorage.getState(Handles.CONTEXT_0, AbstractContextState.class);
            assertTrue(state.isPresent());
            assertEquals(ContextAssociation.DIS, state.get().getContextAssociation());
            state.get().setContextAssociation(ContextAssociation.ASSOC);
            mdibStorage.apply(mock(MdibVersion.class), mock(BigInteger.class),
                    MdibStateModifications.create(MdibStateModifications.Type.CONTEXT)
                            .add(state.get()));
            state = mdibStorage.getState(Handles.CONTEXT_0, AbstractContextState.class);
            assertTrue(state.isPresent());
            assertEquals(ContextAssociation.ASSOC, state.get().getContextAssociation());
        }
        {
            // Read associated context, write to not-associated (i.e., remove)
            var state = mdibStorage.getState(Handles.CONTEXT_0, AbstractContextState.class);
            assertTrue(state.isPresent());
            state.get().setContextAssociation(ContextAssociation.NO);
            var applyResult = mdibStorage.apply(mock(MdibVersion.class), mock(BigInteger.class),
                    MdibStateModifications.create(MdibStateModifications.Type.CONTEXT)
                            .add(state.get()));
            // Expect the state to be part of the report
            assertEquals(1, applyResult.getStates().size());
            var writtenState = applyResult.getStates().get(0);
            assertTrue(writtenState instanceof AbstractContextState);
            assertEquals(ContextAssociation.NO, ((AbstractContextState) writtenState).getContextAssociation());
            state = mdibStorage.getState(Handles.CONTEXT_0, AbstractContextState.class);
            // Expect the state not to be in the MDIB anymore
            assertTrue(state.isEmpty());
        }
        {
            // Write a new context state as not-associated
            var contextState = new PatientContextState();
            contextState.setHandle(Handles.CONTEXT_3);
            contextState.setDescriptorHandle(Handles.CONTEXTDESCRIPTOR_0);

            var applyResult = mdibStorage.apply(mock(MdibVersion.class), mock(BigInteger.class),
                    MdibStateModifications.create(MdibStateModifications.Type.CONTEXT)
                            .add(contextState));
            // Expect the state to be part of the report
            assertEquals(1, applyResult.getStates().size());
            var writtenState = applyResult.getStates().get(0);
            assertTrue(writtenState instanceof AbstractContextState);
            assertNull(((AbstractContextState) writtenState).getContextAssociation());
            var state = mdibStorage.getState(Handles.CONTEXT_3, AbstractContextState.class);
            // Expect the state not to be in the MDIB
            assertTrue(state.isEmpty());
        }
    }

    @Test
    void testParentOfInsertedEntitiesAppearInUpdatedList() {
        applyDescriptionWithVersion(MdibDescriptionModification.Type.INSERT, BigInteger.ZERO);

        {
            // Check that parent MDS appears in updated list
            var modifications = MdibDescriptionModifications.create()
                    .insert(MockModelFactory.createDescriptor(Handles.VMD_1, mock(BigInteger.class), VmdDescriptor.class),
                            MockModelFactory.createState(Handles.VMD_1, mock(BigInteger.class), VmdState.class),
                            Handles.MDS_0);
            var result = mdibStorage.apply(mock(MdibVersion.class), mock(BigInteger.class), mock(BigInteger.class), modifications);

            assertEquals(1, result.getInsertedEntities().size());
            assertEquals(Handles.VMD_1, result.getInsertedEntities().get(0).getHandle());
            assertEquals(1, result.getUpdatedEntities().size());
            assertEquals(Handles.MDS_0, result.getUpdatedEntities().get(0).getHandle());
        }

        {
            // Check that parent VMD does not appear twice in updated list
            var modifications = MdibDescriptionModifications.create()
                    .insert(MockModelFactory.createDescriptor(Handles.CHANNEL_2, mock(BigInteger.class), ChannelDescriptor.class),
                            MockModelFactory.createState(Handles.CHANNEL_2, mock(BigInteger.class), ChannelState.class),
                            Handles.VMD_0)
                    .update(MockModelFactory.createDescriptor(Handles.VMD_0, mock(BigInteger.class), MdsDescriptor.class),
                            MockModelFactory.createState(Handles.VMD_0, mock(BigInteger.class), MdsState.class));
            var result = mdibStorage.apply(mock(MdibVersion.class), mock(BigInteger.class), mock(BigInteger.class), modifications);

            assertEquals(1, result.getInsertedEntities().size());
            assertEquals(Handles.CHANNEL_2, result.getInsertedEntities().get(0).getHandle());
            assertEquals(1, result.getUpdatedEntities().size());
            assertEquals(Handles.VMD_0, result.getUpdatedEntities().get(0).getHandle());
        }

        {
            // Check that existing parent VMD does not appear twice in updated list
            var modifications = MdibDescriptionModifications.create()
                    .insert(MockModelFactory.createDescriptor(Handles.CHANNEL_2, mock(BigInteger.class), ChannelDescriptor.class),
                            MockModelFactory.createState(Handles.CHANNEL_2, mock(BigInteger.class), ChannelState.class),
                            Handles.VMD_0)
                    .update(MockModelFactory.createDescriptor(Handles.VMD_0, mock(BigInteger.class), MdsDescriptor.class),
                            MockModelFactory.createState(Handles.VMD_0, mock(BigInteger.class), MdsState.class));
            var result = mdibStorage.apply(mock(MdibVersion.class), mock(BigInteger.class), mock(BigInteger.class), modifications);

            assertEquals(1, result.getInsertedEntities().size());
            assertEquals(Handles.CHANNEL_2, result.getInsertedEntities().get(0).getHandle());
            assertEquals(1, result.getUpdatedEntities().size());
            assertEquals(Handles.VMD_0, result.getUpdatedEntities().get(0).getHandle());
        }

        {
            // Check that inserted parent VMD does not appear in updated list (it is sufficient to add it to the
            // insertedEntities list
            var modifications = MdibDescriptionModifications.create()
                    .insert(MockModelFactory.createDescriptor(Handles.VMD_2, mock(BigInteger.class), VmdDescriptor.class),
                            MockModelFactory.createState(Handles.VMD_2, mock(BigInteger.class), VmdState.class),
                            Handles.MDS_0)
                    .insert(MockModelFactory.createDescriptor(Handles.CHANNEL_3, mock(BigInteger.class), ChannelDescriptor.class),
                            MockModelFactory.createState(Handles.CHANNEL_3, mock(BigInteger.class), ChannelState.class),
                            Handles.VMD_2);
            var result = mdibStorage.apply(mock(MdibVersion.class), mock(BigInteger.class), mock(BigInteger.class), modifications);

            assertEquals(2, result.getInsertedEntities().size());
            assertEquals(Handles.VMD_2, result.getInsertedEntities().get(0).getHandle());
            assertEquals(Handles.CHANNEL_3, result.getInsertedEntities().get(1).getHandle());
            assertEquals(1, result.getUpdatedEntities().size());
            assertEquals(Handles.MDS_0, result.getUpdatedEntities().get(0).getHandle());
        }
    }
}
