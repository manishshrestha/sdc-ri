package org.somda.sdc.biceps.testutil;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.model.participant.*;

import javax.annotation.Nullable;
import java.util.Arrays;

public class BaseTreeModificationsSet {
    private final MockEntryFactory entryFactory;
    private final DescriptorStateDataGenerator dataGenerator;

    public BaseTreeModificationsSet(MockEntryFactory entryFactory) {
        this.entryFactory = entryFactory;
        this.dataGenerator = new DescriptorStateDataGenerator();
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

    // includes all entities plus data plus exemplary extensions
    public MdibDescriptionModifications createFullyPopulatedTree() {
        return MdibDescriptionModifications.create()
                .insert(dataGenerator.mdsDescriptor(Handles.MDS_0), dataGenerator.mdsState())
                .insert(dataGenerator.clockDescriptor(Handles.CLOCK_0), dataGenerator.clockState(), Handles.MDS_0)
//                .insert(dataGenerator.batteryDescriptor(Handles.BATTERY_0), dataGenerator.batteryState(), Handles.MDS_0)
                .insert(dataGenerator.systemContextDescriptor(Handles.SYSTEMCONTEXT_0), dataGenerator.systemContextState(), Handles.MDS_0)
//                .insert(dataGenerator.patientContextDescriptor(Handles.CONTEXTDESCRIPTOR_0), Arrays.asList(dataGenerator.patientContextState(Handles.CONTEXT_0)), Handles.SYSTEMCONTEXT_0)
                .insert(dataGenerator.locationContextDescriptor(Handles.CONTEXTDESCRIPTOR_1), Arrays.asList(dataGenerator.locationContextState(Handles.CONTEXT_1)), Handles.SYSTEMCONTEXT_0)
//                .insert(dataGenerator.ensembleContextDescriptor(Handles.CONTEXTDESCRIPTOR_3), Arrays.asList(dataGenerator.ensembleContextState(Handles.CONTEXT_3)), Handles.SYSTEMCONTEXT_0)
//                .insert(dataGenerator.alertSystemDescriptor(Handles.ALERTSYSTEM_0), dataGenerator.alertSystemState(), Handles.MDS_0)
//                .insert(dataGenerator.alertConditionDescriptor(Handles.ALERTCONDITION_0, Handles.MDS_0), dataGenerator.alertConditionState(), Handles.ALERTSYSTEM_0)
//                .insert(dataGenerator.limitAlertConditionDescriptor(Handles.ALERTCONDITION_1, Handles.MDS_0), dataGenerator.limitAlertConditionState(), Handles.ALERTSYSTEM_0)
//                .insert(dataGenerator.alertSignalDescriptor(Handles.ALERTSIGNAL_0, Handles.ALERTCONDITION_0, AlertSignalManifestation.VIS), dataGenerator.alertSignalState(), Handles.ALERTSYSTEM_0)
//                .insert(dataGenerator.alertSignalDescriptor(Handles.ALERTSIGNAL_1, Handles.ALERTCONDITION_0, AlertSignalManifestation.AUD), dataGenerator.alertSignalState(), Handles.ALERTSYSTEM_0)
//                .insert(dataGenerator.alertSignalDescriptor(Handles.ALERTSIGNAL_2, Handles.ALERTCONDITION_1, AlertSignalManifestation.VIS), dataGenerator.alertSignalState(), Handles.ALERTSYSTEM_0)
//                .insert(dataGenerator.alertSignalDescriptor(Handles.ALERTSIGNAL_3, Handles.ALERTCONDITION_1, AlertSignalManifestation.AUD), dataGenerator.alertSignalState(), Handles.ALERTSYSTEM_0)
//                .insert(dataGenerator.vmdDescriptor(Handles.VMD_0), dataGenerator.vmdState(), Handles.MDS_0)
//                .insert(dataGenerator.alertSystemDescriptor(Handles.ALERTSYSTEM_1), dataGenerator.alertSystemState(), Handles.MDS_0)
//                .insert(dataGenerator.alertConditionDescriptor(Handles.ALERTCONDITION_2, Handles.MDS_0), dataGenerator.alertConditionState(), Handles.ALERTSYSTEM_0)
//                .insert(dataGenerator.limitAlertConditionDescriptor(Handles.ALERTCONDITION_3, Handles.MDS_0), dataGenerator.limitAlertConditionState(), Handles.ALERTSYSTEM_0)
//                .insert(dataGenerator.alertSignalDescriptor(Handles.ALERTSIGNAL_4, Handles.ALERTCONDITION_2, AlertSignalManifestation.VIS), dataGenerator.alertSignalState(), Handles.ALERTSYSTEM_1)
//                .insert(dataGenerator.alertSignalDescriptor(Handles.ALERTSIGNAL_5, Handles.ALERTCONDITION_2, AlertSignalManifestation.AUD), dataGenerator.alertSignalState(), Handles.ALERTSYSTEM_1)
//                .insert(dataGenerator.alertSignalDescriptor(Handles.ALERTSIGNAL_6, Handles.ALERTCONDITION_3, AlertSignalManifestation.VIS), dataGenerator.alertSignalState(), Handles.ALERTSYSTEM_1)
//                .insert(dataGenerator.alertSignalDescriptor(Handles.ALERTSIGNAL_7, Handles.ALERTCONDITION_3, AlertSignalManifestation.AUD), dataGenerator.alertSignalState(), Handles.ALERTSYSTEM_1)
//                .insert(dataGenerator.channelDescriptor(Handles.CHANNEL_0), dataGenerator.channelState(), Handles.VMD_0)
//                .insert(dataGenerator.numericMetricDescriptor(Handles.METRIC_0), dataGenerator.numericMetricState(), Handles.CHANNEL_0)
//                .insert(dataGenerator.stringMetricDescriptor(Handles.METRIC_1), dataGenerator.stringMetricState(), Handles.CHANNEL_0)
//                .insert(dataGenerator.enumStringMetricDescriptor(Handles.METRIC_2), dataGenerator.enumStringMetricState(), Handles.CHANNEL_0)
//                .insert(dataGenerator.realTimeSampleArrayMetricDescriptor(Handles.METRIC_3), dataGenerator.realTimeSampleArrayMetricState(), Handles.CHANNEL_0)
//                .insert(dataGenerator.distributionSampleArrayMetricDescriptor(Handles.METRIC_4), dataGenerator.distributionSampleArrayMetricState(), Handles.CHANNEL_0)
//                .insert(dataGenerator.scoDescriptor(Handles.SCO_0), dataGenerator.scoState(), Handles.MDS_0)
//                .insert(dataGenerator.activateOperationDescriptor(Handles.OPERATION_0, Handles.MDS_0), dataGenerator.activateOperationState(), Handles.SCO_0)
//                .insert(dataGenerator.setStringOperationDescriptor(Handles.OPERATION_1, Handles.METRIC_1), dataGenerator.setStringOperationState(), Handles.SCO_0)
//                .insert(dataGenerator.setValueOperationDescriptor(Handles.OPERATION_2, Handles.METRIC_0), dataGenerator.setValueOperationState(), Handles.SCO_0)
//                .insert(dataGenerator.setComponentStateOperationDescriptor(Handles.OPERATION_3, Handles.MDS_0), dataGenerator.setComponentStateOperationState(), Handles.SCO_0)
//                .insert(dataGenerator.setMetricStateOperationDescriptor(Handles.OPERATION_4, Handles.METRIC_2), dataGenerator.setMetricStateOperationState(), Handles.SCO_0)
//                .insert(dataGenerator.setAlertStateOperationDescriptor(Handles.OPERATION_5, Handles.ALERTCONDITION_0), dataGenerator.setAlertStateOperationState(), Handles.SCO_0)
//                .insert(dataGenerator.setContextStateOperationDescriptor(Handles.OPERATION_6, Handles.CONTEXTDESCRIPTOR_0), dataGenerator.setContextStateOperationState(), Handles.SCO_0)
//                .insert(dataGenerator.scoDescriptor(Handles.SCO_1), dataGenerator.scoState(), Handles.VMD_0)
//                .insert(dataGenerator.activateOperationDescriptor(Handles.OPERATION_7, Handles.VMD_0), dataGenerator.activateOperationState(), Handles.SCO_1)
//                .insert(dataGenerator.setStringOperationDescriptor(Handles.OPERATION_8, Handles.METRIC_1), dataGenerator.setStringOperationState(), Handles.SCO_1)
//                .insert(dataGenerator.setValueOperationDescriptor(Handles.OPERATION_9, Handles.METRIC_0), dataGenerator.setValueOperationState(), Handles.SCO_1)
//                .insert(dataGenerator.setComponentStateOperationDescriptor(Handles.OPERATION_10, Handles.VMD_0), dataGenerator.setComponentStateOperationState(), Handles.SCO_1)
//                .insert(dataGenerator.setMetricStateOperationDescriptor(Handles.OPERATION_11, Handles.METRIC_2), dataGenerator.setMetricStateOperationState(), Handles.SCO_1)
//                .insert(dataGenerator.setAlertStateOperationDescriptor(Handles.OPERATION_12, Handles.ALERTSIGNAL_7), dataGenerator.setAlertStateOperationState(), Handles.SCO_1)
                ;
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
