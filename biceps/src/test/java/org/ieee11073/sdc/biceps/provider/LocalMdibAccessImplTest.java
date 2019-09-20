package org.ieee11073.sdc.biceps.provider;


import com.google.inject.Injector;
import org.ieee11073.sdc.biceps.UnitTestUtil;
import org.ieee11073.sdc.biceps.common.*;
import org.ieee11073.sdc.biceps.common.access.WriteDescriptionResult;
import org.ieee11073.sdc.biceps.common.event.DescriptionModificationMessage;
import org.ieee11073.sdc.biceps.guice.DefaultBicepsConfigModule;
import org.ieee11073.sdc.biceps.model.participant.*;
import org.ieee11073.sdc.biceps.provider.factory.LocalMdibAccessFactory;
import org.ieee11073.sdc.biceps.testutil.Handles;
import org.ieee11073.sdc.biceps.testutil.MockModelFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LocalMdibAccessImplTest {
    private static final UnitTestUtil UT = new UnitTestUtil(new DefaultBicepsConfigModule() {
        @Override
        protected void customConfigure() {
            // Configure to avoid copying and make comparison easier
            bind(CommonConfig.COPY_MDIB_INPUT,
                    Boolean.class,
                    false);
            bind(CommonConfig.COPY_MDIB_OUTPUT,
                    Boolean.class,
                    false);
        }
    });

    private Injector injector;
    private MdibTypeValidator mdibTypeValidator;
    private LocalMdibAccess mdibAccess;

    @BeforeEach
    void setUp() throws Exception {
        injector = UT.getInjector();
        mdibTypeValidator = injector.getInstance(MdibTypeValidator.class);
        mdibAccess = injector.getInstance(LocalMdibAccessFactory.class)
                .createLocalMdibAccess();
    }

    @Test
    void initialInsertAndRead() throws Exception {
        // Given a base tree to be inserted into a local mdib access
        MdibDescriptionModifications descriptionModifications = setupBaseTree();

        // When the modifications are written to the local mdib access
        MdibAccessObserverSpy updatesSpy = new MdibAccessObserverSpy();
        mdibAccess.registerObserver(updatesSpy);
        WriteDescriptionResult writeDescriptionResult = mdibAccess.writeDescription(descriptionModifications);

        // Then expect the changes to be reflected by the return value as well as the notified event
        assertEquals(descriptionModifications.getModifications().size(), writeDescriptionResult.getInsertedEntities().size());
        assertEquals(0, writeDescriptionResult.getUpdatedEntities().size());
        assertEquals(0, writeDescriptionResult.getDeletedEntities().size());
        assertEquals(1, updatesSpy.getRecordedMessages().size());
        assertEquals(DescriptionModificationMessage.class, updatesSpy.getRecordedMessages().get(0).getClass());
        DescriptionModificationMessage castMessage = (DescriptionModificationMessage) updatesSpy.getRecordedMessages().get(0);
        assertEquals(writeDescriptionResult.getInsertedEntities(), castMessage.getInsertedEntities());
        assertEquals(writeDescriptionResult.getUpdatedEntities(), castMessage.getUpdatedEntities());
        assertEquals(writeDescriptionResult.getDeletedEntities(), castMessage.getDeletedEntities());

        // Further plausibility checks (incl. more read access)
        assertEquals(writeDescriptionResult.getMdibVersion(), castMessage.getMdibAccess().getMdibVersion());
        assertEquals(BigInteger.ONE, castMessage.getMdibAccess().getMdDescriptionVersion());
        assertEquals(BigInteger.ONE, castMessage.getMdibAccess().getMdStateVersion());

        List<PatientContextState> actualPatientContextStates = castMessage.getMdibAccess().getContextStates(Handles.CONTEXTDESCRIPTOR_0, PatientContextState.class);
        assertEquals(1, actualPatientContextStates.size());
        assertEquals(Handles.CONTEXTDESCRIPTOR_0, actualPatientContextStates.get(0).getDescriptorHandle());
        assertEquals(Handles.CONTEXT_0, actualPatientContextStates.get(0).getHandle());
        assertEquals(BigInteger.ZERO, actualPatientContextStates.get(0).getDescriptorVersion());
        assertEquals(BigInteger.ZERO, actualPatientContextStates.get(0).getStateVersion());

        Optional<MdsDescriptor> mdsDescr = castMessage.getMdibAccess().getDescriptor(Handles.MDS_0, MdsDescriptor.class);
        assertTrue(mdsDescr.isPresent());
        assertEquals(Handles.MDS_0, mdsDescr.get().getHandle());
        assertEquals(BigInteger.ZERO, mdsDescr.get().getDescriptorVersion());

        Optional<MdsState> mdsState = castMessage.getMdibAccess().getState(Handles.MDS_0, MdsState.class);
        assertTrue(mdsState.isPresent());
        assertEquals(Handles.MDS_0, mdsState.get().getDescriptorHandle());
        assertEquals(BigInteger.ZERO, mdsState.get().getDescriptorVersion());
        assertEquals(BigInteger.ZERO, mdsState.get().getStateVersion());

        Optional<MdibEntity> mdsEntity = castMessage.getMdibAccess().getEntity(Handles.MDS_0);
        assertTrue(mdsEntity.isPresent());
        assertEquals(Handles.MDS_0, mdsEntity.get().getHandle());
        assertEquals(writeDescriptionResult.getMdibVersion(), mdsEntity.get().getLastChanged());
        assertEquals(mdsDescr.get(), mdsEntity.get().getDescriptor());
        assertEquals(1, mdsEntity.get().getStates().size());
        assertEquals(mdsState.get(), mdsEntity.get().getStates().get(0));

        assertEquals(2, castMessage.getMdibAccess().getRootEntities().size());
        assertEquals(Handles.MDS_0, castMessage.getMdibAccess().getRootEntities().get(0).getHandle());

        assertEquals(8, mdsEntity.get().getChildren().size());
        List<MdibEntity> childrenByType = castMessage.getMdibAccess().getChildrenByType(Handles.MDS_0, VmdDescriptor.class);
        assertEquals(3, childrenByType.size());
        assertEquals(Handles.VMD_0, childrenByType.get(0).getHandle());
        assertEquals(Handles.VMD_1, childrenByType.get(1).getHandle());
        assertEquals(Handles.VMD_2, childrenByType.get(2).getHandle());
        assertEquals(1, castMessage.getMdibAccess().getChildrenByType(Handles.MDS_0, ScoDescriptor.class).size());
        assertEquals(0, castMessage.getMdibAccess().getChildrenByType(Handles.MDS_0, ChannelDescriptor.class).size());

        assertEquals(2, castMessage.getMdibAccess().findEntitiesByType(ChannelDescriptor.class).size());
        assertEquals(5, castMessage.getMdibAccess().findEntitiesByType(AbstractMetricDescriptor.class).size());

        Optional<MdibEntity> channelEntity = castMessage.getMdibAccess().getEntity(Handles.CHANNEL_0);
        assertTrue(channelEntity.isPresent());
        assertTrue(channelEntity.get().getParent().isPresent());
        assertEquals(Handles.VMD_0, channelEntity.get().getParent().get());
    }

    @Test
    void mdibVersioning() {
        // Create - check versions
        // Insert description - check versions
        // Update description - check versions
        // Delete description - check versions
        // Update state - check versions
    }

    @Test
    void insertUpdateDelete() {
        // Insert description - check descr and state versions
        // Update description - check descr and state versions
        // Delete description - check descr and state versions
        // Reinsert description - check descr and state versions
        // Update state - check descr and state versions
    }

    private MdibDescriptionModifications setupBaseTree() throws Exception {
        return MdibDescriptionModifications.create()
                .insert(entry(Handles.MDS_0, MdsDescriptor.class))
                .insert(entry(Handles.MDS_1, MdsDescriptor.class))

                .insert(entry(Handles.VMD_0, VmdDescriptor.class, Handles.MDS_0))
                .insert(entry(Handles.VMD_1, VmdDescriptor.class, Handles.MDS_0))
                .insert(entry(Handles.VMD_2, VmdDescriptor.class, Handles.MDS_0))
                .insert(entry(Handles.BATTERY_0, BatteryDescriptor.class, Handles.MDS_0))
                .insert(entry(Handles.CLOCK_0, ClockDescriptor.class, Handles.MDS_0))
                .insert(entry(Handles.ALERTSYSTEM_0, AlertSystemDescriptor.class, Handles.MDS_0))
                .insert(entry(Handles.SCO_0, ScoDescriptor.class, Handles.MDS_0))
                .insert(entry(Handles.SYSTEMCONTEXT_0, SystemContextDescriptor.class, Handles.MDS_0))

                .insert(entry(Handles.CHANNEL_0, ChannelDescriptor.class, Handles.VMD_0))
                .insert(entry(Handles.CHANNEL_1, ChannelDescriptor.class, Handles.VMD_0))

                .insert(entry(Handles.METRIC_0, NumericMetricDescriptor.class, Handles.CHANNEL_0))
                .insert(entry(Handles.METRIC_1, StringMetricDescriptor.class, Handles.CHANNEL_0))
                .insert(entry(Handles.METRIC_2, EnumStringMetricDescriptor.class, Handles.CHANNEL_0))
                .insert(entry(Handles.METRIC_3, RealTimeSampleArrayMetricDescriptor.class, Handles.CHANNEL_0))
                .insert(entry(Handles.METRIC_4, DistributionSampleArrayMetricDescriptor.class, Handles.CHANNEL_0))

                .insert(entry(Handles.OPERATION_0, ActivateOperationDescriptor.class, Handles.SCO_0))
                .insert(entry(Handles.OPERATION_1, SetStringOperationDescriptor.class, Handles.SCO_0))
                .insert(entry(Handles.OPERATION_2, SetValueOperationDescriptor.class, Handles.SCO_0))
                .insert(entry(Handles.OPERATION_3, SetComponentStateOperationDescriptor.class, Handles.SCO_0))
                .insert(entry(Handles.OPERATION_4, SetMetricStateOperationDescriptor.class, Handles.SCO_0))
                .insert(entry(Handles.OPERATION_5, SetAlertStateOperationDescriptor.class, Handles.SCO_0))
                .insert(entry(Handles.OPERATION_6, SetContextStateOperationDescriptor.class, Handles.SCO_0))

                .insert(contextEntry(Handles.CONTEXTDESCRIPTOR_0, Handles.CONTEXT_0, PatientContextDescriptor.class, Handles.SYSTEMCONTEXT_0))
                .insert(contextEntry(Handles.CONTEXTDESCRIPTOR_1, Handles.CONTEXT_1, LocationContextDescriptor.class, Handles.SYSTEMCONTEXT_0))
                .insert(contextEntry(Handles.CONTEXTDESCRIPTOR_2, Handles.CONTEXT_2, EnsembleContextDescriptor.class, Handles.SYSTEMCONTEXT_0))
                .insert(contextEntry(Handles.CONTEXTDESCRIPTOR_3, Handles.CONTEXT_3, WorkflowContextDescriptor.class, Handles.SYSTEMCONTEXT_0))
                .insert(contextEntry(Handles.CONTEXTDESCRIPTOR_4, Handles.CONTEXT_4, OperatorContextDescriptor.class, Handles.SYSTEMCONTEXT_0))
                .insert(contextEntry(Handles.CONTEXTDESCRIPTOR_5, Handles.CONTEXT_5, MeansContextDescriptor.class, Handles.SYSTEMCONTEXT_0));
    }

    private <T extends AbstractDescriptor, V extends AbstractState> MdibDescriptionModifications.Entry entry(String handle, Class<T> descrClass) throws Exception {
        return entry(handle, descrClass, null);
    }

    private <T extends AbstractDescriptor, V extends AbstractState> MdibDescriptionModifications.Entry entry(String handle, Class<T> descrClass, @Nullable String parentHandle) throws Exception {
        Class<V> stateClass = mdibTypeValidator.resolveStateType(descrClass);
        return new MdibDescriptionModifications.Entry(
                descriptor(handle, descrClass),
                state(handle, stateClass),
                parentHandle);
    }

    private <T extends AbstractDescriptor, V extends AbstractContextState> MdibDescriptionModifications.MultiStateEntry contextEntry(String handle, String stateHandle, Class<T> descrClass, String parentHandle) throws Exception {
        Class<V> stateClass = mdibTypeValidator.resolveStateType(descrClass);
        return new MdibDescriptionModifications.MultiStateEntry(
                descriptor(handle, descrClass),
                Collections.singletonList(MockModelFactory.createContextState(stateHandle, handle, stateClass)),
                parentHandle);
    }

    private <T extends AbstractDescriptor> T descriptor(String handle, Class<T> theClass) throws Exception {
        return MockModelFactory.createDescriptor(handle, theClass);
    }

    private <T extends AbstractState> T state(String handle, Class<T> theClass) throws Exception {
        return MockModelFactory.createState(handle, theClass);
    }
}