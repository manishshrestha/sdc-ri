package org.somda.sdc.biceps.provider.preprocessing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.biceps.UnitTestUtil;
import org.somda.sdc.biceps.common.*;
import org.somda.sdc.biceps.common.storage.factory.MdibStorageFactory;
import org.somda.sdc.biceps.common.storage.MdibStorage;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.biceps.testutil.BaseTreeModificationsSet;
import org.somda.sdc.biceps.testutil.Handles;
import org.somda.sdc.biceps.testutil.MockEntryFactory;
import org.somda.sdc.biceps.testutil.MockModelFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.org.somda.common.LoggingTestWatcher;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(LoggingTestWatcher.class)
class VersionHandlerTest {
    private static final UnitTestUtil UT = new UnitTestUtil();

    private MdibStorage mdibStorage;
    private VersionHandler versionHandler;
    private String mdsHandle;
    private String vmdHandle;
    private MdsDescriptor mdsDescriptor;
    private MdsState mdsState;
    private VmdDescriptor vmdDescriptor;
    private VmdState vmdState;
    private String systemContextHandle;
    private SystemContextDescriptor systemContextDescriptor;
    private SystemContextState systemContextState;
    private String patientContextHandle;
    private PatientContextDescriptor patientContextDescriptor;
    private PatientContextState patientContextState1;
    private PatientContextState patientContextState2;
    private String patientContextStateHandle1;
    private String patientContextStateHandle2;

    private MockEntryFactory mockEntryFactory;
    @BeforeEach
    void beforeEach() throws Exception {
        // Given a version handler and sample input
        mdibStorage = UT.getInjector().getInstance(MdibStorageFactory.class).createMdibStorage();
        versionHandler = UT.getInjector().getInstance(VersionHandler.class);
        mockEntryFactory = new MockEntryFactory(UT.getInjector().getInstance(MdibTypeValidator.class));

        mdsHandle = "mds";
        mdsDescriptor = MockModelFactory.createDescriptor(mdsHandle, BigInteger.valueOf(-1), MdsDescriptor.builder()).build();
        mdsState = MockModelFactory.createState(mdsHandle, BigInteger.valueOf(-1), BigInteger.valueOf(-1), MdsState.builder()).build();

        vmdHandle = "vmd";
        vmdDescriptor = MockModelFactory.createDescriptor(vmdHandle, BigInteger.valueOf(-1), VmdDescriptor.builder()).build();
        vmdState = MockModelFactory.createState(vmdHandle, BigInteger.valueOf(-1), BigInteger.valueOf(-1), VmdState.builder()).build();

        systemContextHandle = "systemcontext";
        systemContextDescriptor = MockModelFactory.createDescriptor(systemContextHandle, BigInteger.valueOf(-1),
                SystemContextDescriptor.builder()).build();
        systemContextState = MockModelFactory.createState(systemContextHandle, BigInteger.valueOf(-1), BigInteger.valueOf(-1),
                SystemContextState.builder()).build();

        patientContextHandle = "patientcontext";
        patientContextStateHandle1 = "patientcontextstate1";
        patientContextStateHandle2 = "patientcontextstate2";
        patientContextHandle = "patientcontext";
        patientContextDescriptor = MockModelFactory.createDescriptor(patientContextHandle, BigInteger.valueOf(-1),
                PatientContextDescriptor.builder()).build();
        patientContextState1 = MockModelFactory.createContextState(patientContextStateHandle1, patientContextHandle,
                BigInteger.valueOf(-1), BigInteger.valueOf(-1), PatientContextState.builder()).build();
        patientContextState2 = MockModelFactory.createContextState(patientContextStateHandle2, patientContextHandle,
                BigInteger.valueOf(-1), BigInteger.valueOf(-1), PatientContextState.builder()).build();
    }

    void setupStorage() {
        var baseTree = new BaseTreeModificationsSet(mockEntryFactory).createBaseTree();
        mdibStorage.apply(mock(MdibVersion.class), mock(BigInteger.class), mock(BigInteger.class), baseTree.getModifications());
    }

    @Test
    @DisplayName("Inserts a single state descriptor and checks that the parent is updated.")
    void testDescriptorVersioningSingleStateInsertCausesUpdate() throws Exception {
        setupStorage();

        final MdibDescriptionModifications modifications = MdibDescriptionModifications.create();
        modifications.insert(mockEntryFactory.entry(Handles.CHANNEL_3, ChannelDescriptor.builder(), ChannelState.builder(), Handles.VMD_0));
        final var parentEntity = mdibStorage.getEntity(Handles.VMD_0).orElseThrow();

        var appliedModifications = versionHandler.beforeFirstModification(modifications.getModifications(), mdibStorage);
        assertEquals(1, appliedModifications.size());

        appliedModifications = versionHandler.process(appliedModifications, mdibStorage);
        assertEquals(2, appliedModifications.size());

        // also check that update was added to modifications
        assertEquals(2, appliedModifications.size());
        assertEquals(MdibDescriptionModification.Type.UPDATE, appliedModifications.get(1).getModificationType());
        assertEquals(Handles.VMD_0, appliedModifications.get(1).getHandle());
        assertEquals(
            parentEntity.getDescriptor().getDescriptorVersion().add(BigInteger.ONE),
            appliedModifications.get(1).getDescriptor().getDescriptorVersion()
        );
        assertEquals(
            parentEntity.getDescriptor().getDescriptorVersion().add(BigInteger.ONE),
            appliedModifications.get(1).getStates().get(0).getDescriptorVersion()
        );
        assertEquals(
            parentEntity.getStates().get(0).getStateVersion().add(BigInteger.ONE),
            appliedModifications.get(1).getStates().get(0).getStateVersion()
        );

        appliedModifications = versionHandler.afterLastModification(appliedModifications, mdibStorage);
        assertEquals(2, appliedModifications.size());

        // make sure version handler cached the updated parent and also flushes the cache afterwards
        assertTrue(versionHandler.getUpdatedVersions().containsKey(Handles.VMD_0));
        versionHandler.beforeFirstModification(appliedModifications, mdibStorage);
        assertFalse(versionHandler.getUpdatedVersions().containsKey(Handles.VMD_0));
    }

    @Test
    @DisplayName("Inserts a multi state descriptor with no states and checks that the parent is updated")
    void testDescriptorVersioningMultiStateWithoutStatesInsertCausesUpdate() throws Exception {
        setupStorage();

        var newDescriptorHandle = "What's orange and sounds like a parrot?\n"
            + "\n"
            + "A carrot.";

        final MdibDescriptionModifications modifications = MdibDescriptionModifications.create();
        modifications.insert(mockEntryFactory.contextEntry(newDescriptorHandle, Collections.emptyList(), MeansContextDescriptor.builder(), MeansContextState.builder(), Handles.SYSTEMCONTEXT_0));
        final var parentEntity = mdibStorage.getEntity(Handles.SYSTEMCONTEXT_0).orElseThrow();

        var appliedModifications = versionHandler.beforeFirstModification(modifications.getModifications(), mdibStorage);
        assertEquals(1, appliedModifications.size());

        appliedModifications = versionHandler.process(appliedModifications, mdibStorage);
        assertEquals(2, appliedModifications.size());

        // also check that update was added to modifications
        assertEquals(2, appliedModifications.size());
        assertEquals(MdibDescriptionModification.Type.UPDATE, appliedModifications.get(1).getModificationType());
        assertEquals(Handles.SYSTEMCONTEXT_0, appliedModifications.get(1).getHandle());
        assertEquals(
            parentEntity.getDescriptor().getDescriptorVersion().add(BigInteger.ONE),
            appliedModifications.get(1).getDescriptor().getDescriptorVersion()
        );

        appliedModifications = versionHandler.afterLastModification(appliedModifications, mdibStorage);
        assertEquals(2, appliedModifications.size());

        // make sure version handler cached the updated parent and also flushes the cache afterwards
        assertTrue(versionHandler.getUpdatedVersions().containsKey(Handles.SYSTEMCONTEXT_0));
        versionHandler.beforeFirstModification(appliedModifications, mdibStorage);
        assertFalse(versionHandler.getUpdatedVersions().containsKey(Handles.SYSTEMCONTEXT_0));

    }

    @Test
    @DisplayName("Inserts a multi state descriptor with states and checks that the parent is updated")
    void testDescriptorVersioningMultiStateInsertCausesUpdate() throws Exception {
        setupStorage();

        var newDescriptorHandle = "What's orange and sounds like a parrot?\n"
            + "\n"
            + "A carrot.";
        var newStateHandle = "oh...So_this_is_a_new_handle,huh?I_mean...kinda_disappointing,don't_you_think?";

        final MdibDescriptionModifications modifications = MdibDescriptionModifications.create();
        modifications.insert(mockEntryFactory.contextEntry(
            newDescriptorHandle, List.of(newStateHandle),
            MeansContextDescriptor.builder(), MeansContextState.builder(), Handles.SYSTEMCONTEXT_0
        ));
        final var parentEntity = mdibStorage.getEntity(Handles.SYSTEMCONTEXT_0).orElseThrow();

        var appliedModifications = versionHandler.beforeFirstModification(modifications.getModifications(), mdibStorage);
        assertEquals(1, appliedModifications.size());

        appliedModifications = versionHandler.process(appliedModifications, mdibStorage);
        assertEquals(2, appliedModifications.size());

        // also check that update was added to modifications
        assertEquals(2, appliedModifications.size());
        assertEquals(MdibDescriptionModification.Type.UPDATE, appliedModifications.get(1).getModificationType());
        assertEquals(Handles.SYSTEMCONTEXT_0, appliedModifications.get(1).getHandle());
        assertEquals(
            parentEntity.getDescriptor().getDescriptorVersion().add(BigInteger.ONE),
            appliedModifications.get(1).getDescriptor().getDescriptorVersion()
        );

        appliedModifications = versionHandler.afterLastModification(appliedModifications, mdibStorage);
        assertEquals(2, appliedModifications.size());

        // make sure version handler cached the updated parent and also flushes the cache afterwards
        assertTrue(versionHandler.getUpdatedVersions().containsKey(Handles.SYSTEMCONTEXT_0));
        versionHandler.beforeFirstModification(appliedModifications, mdibStorage);
        assertFalse(versionHandler.getUpdatedVersions().containsKey(Handles.SYSTEMCONTEXT_0));
    }

    @Test
    @DisplayName("Inserts a single state descriptor checks that the parent is updated, but also the manually added update later on.")
    void testDescriptorVersioningSingleStateInsertUpdatesLaterUpdateVersions() throws Exception {
        setupStorage();

        final MdibDescriptionModifications modifications = MdibDescriptionModifications.create();
        modifications.insert(mockEntryFactory.entry(Handles.CHANNEL_3, ChannelDescriptor.builder(), ChannelState.builder(), Handles.VMD_0));
        final var parentEntity = mdibStorage.getEntity(Handles.VMD_0).orElseThrow();

        var updatedDescriptor = ((VmdDescriptor) parentEntity.getDescriptor())
            .newCopyBuilder()
            .withSafetyClassification(SafetyClassification.MED_C)
            .build();
        modifications.update(updatedDescriptor, parentEntity.getStates().get(0));

        var appliedModifications = versionHandler.beforeFirstModification(modifications.getModifications(), mdibStorage);
        assertEquals(2, appliedModifications.size());

        appliedModifications = versionHandler.process(appliedModifications, mdibStorage);
        assertEquals(3, appliedModifications.size());

        // also check that update was added to modifications
        final List<MdibDescriptionModification> finalAppliedModifications = appliedModifications;
        IntStream.rangeClosed(1, 2).forEach(index -> {
            assertEquals(MdibDescriptionModification.Type.UPDATE, finalAppliedModifications.get(index).getModificationType());
            assertEquals(Handles.VMD_0, finalAppliedModifications.get(index).getHandle());
            assertEquals(
                parentEntity.getDescriptor().getDescriptorVersion().add(BigInteger.ONE),
                finalAppliedModifications.get(index).getDescriptor().getDescriptorVersion()
            );
            assertEquals(
                parentEntity.getDescriptor().getDescriptorVersion().add(BigInteger.ONE),
                finalAppliedModifications.get(index).getStates().get(0).getDescriptorVersion()
            );
            assertEquals(
                parentEntity.getStates().get(0).getStateVersion().add(BigInteger.ONE),
                finalAppliedModifications.get(index).getStates().get(0).getStateVersion()
            );
            if (index == 1) {
                // make sure update bumps parent version but does not add safety classification
                assertNull(finalAppliedModifications.get(index).getDescriptor().getSafetyClassification());
            } else if (index == 2) {
                // make sure manual update bumps parent version and sets safety classification
                assertEquals(SafetyClassification.MED_C, finalAppliedModifications.get(index).getDescriptor().getSafetyClassification());
            }
        });

        appliedModifications = versionHandler.afterLastModification(appliedModifications, mdibStorage);
        assertEquals(3, appliedModifications.size());

        // make sure version handler cached the updated parent and also flushes the cache afterwards
        assertTrue(versionHandler.getUpdatedVersions().containsKey(Handles.VMD_0));
        versionHandler.beforeFirstModification(appliedModifications, mdibStorage);
        assertFalse(versionHandler.getUpdatedVersions().containsKey(Handles.VMD_0));
    }


    @Test
    @DisplayName("Updates a multi state descriptor and adds a new state as a side effect, ensures versions are correctly updated, i.e. only the new states is not incremented.")
    void testDescriptorVersioningUpdateInsertNewMultiStateState() throws Exception {
        setupStorage();

        var modifications = MdibDescriptionModifications.create();

        // get current patient context entity
        var patientEntity = mdibStorage.getEntity(Handles.CONTEXTDESCRIPTOR_0).orElseThrow();
        var contexts = patientEntity.getStates().stream().map(it -> (PatientContextState) it).collect(Collectors.toList());

        var newStateHandle = "oh...So_this_is_a_new_handle,huh?I_mean...kinda_disappointing,don't_you_think?";

        var newContexts = new ArrayList<>(contexts);
        newContexts.add(PatientContextState.builder().withDescriptorHandle(Handles.CONTEXTDESCRIPTOR_0).withHandle(newStateHandle).build());

        var oldContextsMap = contexts.stream()
            .collect(Collectors.toMap(AbstractMultiState::getHandle, e -> e));

        modifications.update(
            patientEntity.getDescriptor(),
            newContexts
        );

        var processedModifications = versionHandler.beforeFirstModification(modifications.getModifications(), mdibStorage);
        assertEquals(1, processedModifications.size());

        processedModifications = versionHandler.process(modifications.getModifications(), mdibStorage);
        assertEquals(1, processedModifications.size());

        // make sure update bumps parent version
        assertEquals(patientEntity.getDescriptor().getDescriptorVersion().add(BigInteger.ONE), processedModifications.get(0).getDescriptor().getDescriptorVersion());
        assertEquals(oldContextsMap.size() + 1, processedModifications.get(0).getStates().size());

        final List<MdibDescriptionModification> finalProcessedModifications = processedModifications;
        finalProcessedModifications.get(0).getStates()
            .stream().map(it -> (AbstractContextState) it)
            .forEach(state -> {
                if (newStateHandle.equals(state.getHandle())) {
                    // new context state should be 0
                    assertEquals(BigInteger.ZERO, state.getStateVersion());
                } else {
                    var oldState = oldContextsMap.get(state.getHandle());

                    assertEquals(oldState.getStateVersion().add(BigInteger.ONE), state.getStateVersion());
                }
                assertEquals(finalProcessedModifications.get(0).getDescriptor().getDescriptorVersion(), state.getDescriptorVersion());
            });

    }

    @Test
    @DisplayName("Updates multiple single states with incorrect versions, checks for correctness of versions of all updates after processing.")
    void testDescriptorVersioningUpdates() throws VersioningException {
        setupStorage();

        var vmdEntity = mdibStorage.getEntity(Handles.VMD_0).orElseThrow();
        var channelEntity = mdibStorage.getEntity(Handles.CHANNEL_0).orElseThrow();
        var metricEntity = mdibStorage.getEntity(Handles.METRIC_0).orElseThrow();

        var modifications = MdibDescriptionModifications.create();
        modifications.update(
            vmdEntity.getDescriptor().newCopyBuilder().withDescriptorVersion(BigInteger.valueOf(4)).build(),
            vmdEntity.getStates().get(0)
        );
        modifications.update(
            channelEntity.getDescriptor().newCopyBuilder().withDescriptorVersion(BigInteger.valueOf(12)).build(),
            channelEntity.getStates().get(0)
        );
        modifications.update(
            metricEntity.getDescriptor().newCopyBuilder().withDescriptorVersion(BigInteger.valueOf(5)).build(),
            metricEntity.getStates().get(0)
        );

        var appliedModifications = versionHandler.beforeFirstModification(modifications.getModifications(), mdibStorage);
        assertEquals(3, appliedModifications.size());

        appliedModifications = versionHandler.process(appliedModifications, mdibStorage);
        assertEquals(3, appliedModifications.size());

        // check vmd version
        assertTrue(appliedModifications.get(0).getDescriptor() instanceof VmdDescriptor);
        assertEquals(vmdEntity.getDescriptor().getDescriptorVersion().add(BigInteger.ONE), appliedModifications.get(0).getDescriptor().getDescriptorVersion());
        assertEquals(vmdEntity.getDescriptor().getDescriptorVersion().add(BigInteger.ONE), appliedModifications.get(0).getStates().get(0).getDescriptorVersion());
        assertEquals(vmdEntity.getStates().get(0).getStateVersion().add(BigInteger.ONE), appliedModifications.get(0).getStates().get(0).getStateVersion());

        // check channel version
        assertTrue(appliedModifications.get(1).getDescriptor() instanceof ChannelDescriptor);
        assertEquals(channelEntity.getDescriptor().getDescriptorVersion().add(BigInteger.ONE), appliedModifications.get(1).getDescriptor().getDescriptorVersion());
        assertEquals(channelEntity.getDescriptor().getDescriptorVersion().add(BigInteger.ONE), appliedModifications.get(1).getStates().get(0).getDescriptorVersion());
        assertEquals(channelEntity.getStates().get(0).getStateVersion().add(BigInteger.ONE), appliedModifications.get(1).getStates().get(0).getStateVersion());

        // check vmd version
        assertTrue(appliedModifications.get(2).getDescriptor() instanceof NumericMetricDescriptor);
        assertEquals(metricEntity.getDescriptor().getDescriptorVersion().add(BigInteger.ONE), appliedModifications.get(2).getDescriptor().getDescriptorVersion());
        assertEquals(metricEntity.getDescriptor().getDescriptorVersion().add(BigInteger.ONE), appliedModifications.get(2).getStates().get(0).getDescriptorVersion());
        assertEquals(metricEntity.getStates().get(0).getStateVersion().add(BigInteger.ONE), appliedModifications.get(2).getStates().get(0).getStateVersion());
    }

    @Test
    @DisplayName("Updates a metric descriptor, then deletes and reinserts it, ensuring correct versioning all the time.")
    void testDescriptorDeleteCausesUpdate() throws VersioningException {
        setupStorage();

        var channelEntity = mdibStorage.getEntity(Handles.CHANNEL_0).orElseThrow();
        var metricEntity = mdibStorage.getEntity(Handles.METRIC_0).orElseThrow();

        // delete metric
        {
            var modifications = MdibDescriptionModifications.create();
            modifications.delete(metricEntity.getHandle());

            var appliedModifications = versionHandler.beforeFirstModification(modifications.getModifications(), mdibStorage);
            assertEquals(1, appliedModifications.size());

            appliedModifications = versionHandler.process(appliedModifications, mdibStorage);
            // parent update added
            assertEquals(2, appliedModifications.size());

            // check delete
            assertEquals(metricEntity.getHandle(), appliedModifications.get(0).getHandle());

            // check update
            assertTrue(appliedModifications.get(1).getDescriptor() instanceof ChannelDescriptor);
            assertEquals(channelEntity.getDescriptor().getDescriptorVersion().add(BigInteger.ONE), appliedModifications.get(1).getDescriptor().getDescriptorVersion());
            assertEquals(channelEntity.getDescriptor().getDescriptorVersion().add(BigInteger.ONE), appliedModifications.get(1).getStates().get(0).getDescriptorVersion());
            assertEquals(channelEntity.getStates().get(0).getStateVersion().add(BigInteger.ONE), appliedModifications.get(1).getStates().get(0).getStateVersion());
        }
    }

    @Test
    void testStateVersioningComponent() throws VersioningException {
        setupStorage();

        var vmdEntity = mdibStorage.getEntity(Handles.VMD_0).orElseThrow();
        var clockEntity = mdibStorage.getEntity(Handles.CLOCK_0).orElseThrow();

        var vmdCode = CodedValue.builder().withCode("yolocode").build();
        var clockBadVersion = BigInteger.valueOf(500);

        // change vmd state a little
        var vmdState = ((VmdState)vmdEntity.getStates().get(0)).newCopyBuilder()
            .withOperatingJurisdiction(
                OperatingJurisdiction.builder()
                    .withType(vmdCode)
                .build()
            )
            .build();

        var clockState = ((ClockState) clockEntity.getStates().get(0)).newCopyBuilder()
            .withDescriptorVersion(clockBadVersion)
            .withStateVersion(clockBadVersion)
            .build();

        var modifications = MdibStateModifications.create(MdibStateModifications.Type.COMPONENT, 1);
        modifications.add(vmdState);
        modifications.add(clockState);

        versionHandler.beforeFirstModification(modifications, mdibStorage);
        assertEquals(2, modifications.getStates().size());

        versionHandler.process(modifications, mdibStorage);
        assertEquals(2, modifications.getStates().size());

        // check version correct
        assertEquals(vmdEntity.getDescriptor().getDescriptorVersion(), modifications.getStates().get(0).getDescriptorVersion());
        assertEquals(vmdEntity.getStates().get(0).getStateVersion().add(BigInteger.ONE), modifications.getStates().get(0).getStateVersion());
        // assert update present
        assertEquals(vmdCode, ((VmdState) modifications.getStates().get(0)).getOperatingJurisdiction().getType());

        // check version correct
        assertEquals(clockEntity.getDescriptor().getDescriptorVersion(), modifications.getStates().get(1).getDescriptorVersion());
        assertEquals(clockEntity.getStates().get(0).getStateVersion().add(BigInteger.ONE), modifications.getStates().get(1).getStateVersion());

        assertNotEquals(clockBadVersion, modifications.getStates().get(1).getDescriptorVersion());
        assertNotEquals(clockBadVersion.add(BigInteger.ONE), modifications.getStates().get(1).getDescriptorVersion());

        assertNotEquals(clockBadVersion, modifications.getStates().get(1).getStateVersion());
        assertNotEquals(clockBadVersion.add(BigInteger.ONE), modifications.getStates().get(1).getStateVersion());
    }

    @Test
    void testStateVersioningComponentMultiple() throws VersioningException {
        setupStorage();

        var vmdEntity = mdibStorage.getEntity(Handles.VMD_0).orElseThrow();

        var vmdCode = CodedValue.builder().withCode("yolocode").build();
        var badVersion = BigInteger.valueOf(500);

        // change vmd state a little
        var vmdState = ((VmdState)vmdEntity.getStates().get(0)).newCopyBuilder()
            .withOperatingJurisdiction(
                OperatingJurisdiction.builder()
                    .withType(vmdCode)
                    .build()
            ).withDescriptorVersion(badVersion)
            .withStateVersion(badVersion)
            .build();

        var modifications = MdibStateModifications.create(MdibStateModifications.Type.COMPONENT);
        modifications.add(vmdState);
        modifications.add(vmdState);

        versionHandler.beforeFirstModification(modifications, mdibStorage);
        assertEquals(2, modifications.getStates().size());

        versionHandler.process(modifications, mdibStorage);
        // nothing added
        assertEquals(2, modifications.getStates().size());

        var counter = 0;

        IntStream.rangeClosed(0, 1).forEach(idx -> {
            // check version correct
            assertEquals(vmdEntity.getDescriptor().getDescriptorVersion(), modifications.getStates().get(idx).getDescriptorVersion());
            assertEquals(vmdEntity.getStates().get(0).getStateVersion().add(BigInteger.ONE), modifications.getStates().get(idx).getStateVersion());
            // assert update present
            assertEquals(vmdCode, ((VmdState)modifications.getStates().get(idx)).getOperatingJurisdiction().getType());
        });
    }

    @Test
    void testStateVersioningContext() throws VersioningException {
        setupStorage();

        var newStateHandle = "DingDong";

        var patientContextEntity = mdibStorage.getEntity(Handles.CONTEXTDESCRIPTOR_0).orElseThrow();

        var bindingVersion = BigInteger.valueOf(5);
        var badVersion = BigInteger.valueOf(650);

        var firstState = ((PatientContextState) patientContextEntity.getStates().get(0)).newCopyBuilder()
            .withBindingMdibVersion(bindingVersion)
            .withDescriptorVersion(badVersion)
            .withStateVersion(badVersion)
            .build();

        var newState = PatientContextState.builder()
            .withDescriptorHandle(Handles.CONTEXTDESCRIPTOR_0)
            .withHandle(newStateHandle)
            .withDescriptorVersion(badVersion)
            .withStateVersion(badVersion)
            .build();

        var modifications = MdibStateModifications.create(MdibStateModifications.Type.CONTEXT);
        modifications.add(firstState);
        modifications.add(newState);

        versionHandler.beforeFirstModification(modifications, mdibStorage);
        assertEquals(2, modifications.getStates().size());

        versionHandler.process(modifications, mdibStorage);
        // nothing changed
        assertEquals(2, modifications.getStates().size());

        PatientContextState state1 = (PatientContextState) modifications.getStates().get(0);
        assertEquals(Handles.CONTEXT_0, state1.getHandle());
        assertEquals(BigInteger.ONE, state1.getStateVersion());
        assertEquals(patientContextEntity.getDescriptor().getDescriptorVersion(), state1.getDescriptorVersion());
        assertEquals(bindingVersion, state1.getBindingMdibVersion());

        PatientContextState state2 = (PatientContextState) modifications.getStates().get(1);
        assertEquals(newStateHandle, state2.getHandle());
        assertEquals(BigInteger.ZERO, state2.getStateVersion());
        assertEquals(patientContextEntity.getDescriptor().getDescriptorVersion(), state2.getDescriptorVersion());
    }

    @Test
    void testStateVersioningContextMultiple() throws VersioningException {
        setupStorage();

        var patientContextEntity = mdibStorage.getEntity(Handles.CONTEXTDESCRIPTOR_0).orElseThrow();

        var bindingVersion = BigInteger.valueOf(5);
        var badVersion = BigInteger.valueOf(650);


        var firstState = ((PatientContextState) patientContextEntity.getStates().get(0)).newCopyBuilder()
            .withBindingMdibVersion(bindingVersion)
            .withDescriptorVersion(badVersion)
            .withStateVersion(badVersion)
            .build();

        var modifications = MdibStateModifications.create(MdibStateModifications.Type.CONTEXT);
        modifications.add(firstState);
        modifications.add(firstState);

        versionHandler.beforeFirstModification(modifications, mdibStorage);
        assertEquals(2, modifications.getStates().size());

        versionHandler.process(modifications, mdibStorage);
        // nothing changed
        assertEquals(2, modifications.getStates().size());

        IntStream.rangeClosed(0, 1).forEach(idx -> {
            var state = (PatientContextState) modifications.getStates().get(0);
            // check handle equals old handle
            assertEquals(patientContextEntity.getStates(PatientContextState.class).get(0).getHandle(), state.getHandle());
            // check version correct
            assertEquals(BigInteger.ONE, state.getStateVersion());
            assertEquals(patientContextEntity.getDescriptor().getDescriptorVersion(), state.getDescriptorVersion());
            // assert update present
            assertEquals(bindingVersion, state.getBindingMdibVersion());
        });
    }

    @Test
    @DisplayName("Inserts a single state descriptor and checks that the versions is saved, then checks that starting a state update flushes saved versions.")
    void testStateUpdateBeforeFlushesSavedVersions() throws Exception {
        setupStorage();

        var newDescriptorHandle = "Booooo";

        var modifications = MdibDescriptionModifications.create();
        modifications.insert(mockEntryFactory.entry(newDescriptorHandle, ChannelDescriptor.builder(), ChannelState.builder(), Handles.VMD_0));

        var parentEntity = mdibStorage.getEntity(Handles.VMD_0).orElseThrow();

        var appliedModifications = versionHandler.beforeFirstModification(modifications.getModifications(), mdibStorage);
        assertEquals(1, appliedModifications.size());

        appliedModifications = versionHandler.process(appliedModifications, mdibStorage);
        // update added
        assertEquals(2, appliedModifications.size());
        assertEquals(MdibDescriptionModification.Type.UPDATE, appliedModifications.get(1).getModificationType());

        assertEquals(parentEntity.getDescriptor().getDescriptorVersion().add(BigInteger.ONE), appliedModifications.get(1).getDescriptor().getDescriptorVersion());

        var state = appliedModifications.get(1).getStates().get(0);
        assertEquals(parentEntity.getStates().get(0).getStateVersion().add(BigInteger.ONE), state.getStateVersion());
        assertEquals(appliedModifications.get(1).getDescriptor().getDescriptorVersion(), state.getDescriptorVersion());
    }
}