package org.somda.sdc.biceps.testutil;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.model.participant.*;

import javax.annotation.Nullable;

public class BaseTreeModificationsSet {
    private final MockEntryFactory entryFactory;

    public BaseTreeModificationsSet(MockEntryFactory entryFactory) {
        this.entryFactory = entryFactory;
    }

    public MdibDescriptionModifications createBaseTree() {
        try {
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private <T extends AbstractDescriptor, V extends AbstractState> MdibDescriptionModifications.Entry entry(String handle, Class<T> descrClass) throws Exception {
        return entry(handle, descrClass, null);
    }

    private <T extends AbstractDescriptor, V extends AbstractState> MdibDescriptionModifications.Entry entry(String handle, Class<T> descrClass, @Nullable String parentHandle) throws Exception {
        return entryFactory.entry(handle, descrClass, parentHandle);
    }

    private <T extends AbstractDescriptor, V extends AbstractContextState> MdibDescriptionModifications.MultiStateEntry contextEntry(String handle, String stateHandle, Class<T> descrClass, String parentHandle) throws Exception {
        return entryFactory.contextEntry(handle, stateHandle, descrClass, parentHandle);
    }

    private <T extends AbstractDescriptor> T descriptor(String handle, Class<T> theClass) {
        return MockModelFactory.createDescriptor(handle, theClass);
    }

    private <T extends AbstractState> T state(String handle, Class<T> theClass) {
        return MockModelFactory.createState(handle, theClass);
    }
}
