package org.somda.sdc.biceps.testutil;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.biceps.model.participant.factory.CodedValueFactory;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Consumer;

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
                    .insert(entry(Handles.MDS_0, t -> t.withType(CodedValueFactory.createIeeeCodedValue("70001")), t -> {}, MdsDescriptor.builder(), MdsState.builder()))
                    .insert(entry(Handles.MDS_1, t -> t.withType(CodedValueFactory.createIeeeCodedValue("70002")), t -> {}, MdsDescriptor.builder(), MdsState.builder()))

                    .insert(entry(Handles.VMD_0, VmdDescriptor.builder(), VmdState.builder(), Handles.MDS_0))
                    .insert(entry(Handles.VMD_1, VmdDescriptor.builder(), VmdState.builder(), Handles.MDS_0))
                    .insert(entry(Handles.VMD_2, VmdDescriptor.builder(), VmdState.builder(), Handles.MDS_0))
                    .insert(entry(Handles.BATTERY_0, BatteryDescriptor.builder(), BatteryState.builder(), Handles.MDS_0))
                    .insert(entry(Handles.CLOCK_0, ClockDescriptor.builder(), ClockState.builder(), Handles.MDS_0))
                    .insert(entry(Handles.ALERTSYSTEM_0, AlertSystemDescriptor.builder(), AlertSystemState.builder(), t -> {}, t -> {
                        t.withActivationState(AlertActivation.ON);
                    }, Handles.MDS_0))
                    .insert(entry(Handles.SCO_0, ScoDescriptor.builder(), ScoState.builder(), Handles.MDS_0))
                    .insert(entry(Handles.SYSTEMCONTEXT_0, SystemContextDescriptor.builder(), SystemContextState.builder(), Handles.MDS_0))

                    .insert(entry(Handles.CHANNEL_0, ChannelDescriptor.builder(), ChannelState.builder(), Handles.VMD_0))
                    .insert(entry(Handles.CHANNEL_1, ChannelDescriptor.builder(), ChannelState.builder(), Handles.VMD_0))

                    .insert(entry(Handles.METRIC_0, NumericMetricDescriptor.builder(), NumericMetricState.builder(), t -> {
                        t.withResolution(BigDecimal.ONE);
                        t.withMetricCategory(MetricCategory.UNSPEC);
                        t.withMetricAvailability(MetricAvailability.INTR);
                        t.withUnit(CodedValueFactory.createIeeeCodedValue("500"));
                    }, t -> {}, Handles.CHANNEL_0))
                    .insert(entry(Handles.METRIC_1, StringMetricDescriptor.builder(), StringMetricState.builder(), t -> {
                        t.withMetricCategory(MetricCategory.UNSPEC);
                        t.withMetricAvailability(MetricAvailability.INTR);
                        t.withUnit(CodedValueFactory.createIeeeCodedValue("500"));
                    }, t -> {}, Handles.CHANNEL_0))
                    .insert(entry(Handles.METRIC_2, EnumStringMetricDescriptor.builder(), EnumStringMetricState.builder(), t -> {
                        t.withMetricCategory(MetricCategory.UNSPEC);
                        t.withMetricAvailability(MetricAvailability.INTR);
                        t.withUnit(CodedValueFactory.createIeeeCodedValue("500"));
                        var allowedValue = EnumStringMetricDescriptor.AllowedValue.builder()
                            .withValue("sample").build();
                        t.addAllowedValue(allowedValue);
                    }, t -> {}, Handles.CHANNEL_0))
                    .insert(entry(Handles.METRIC_3, RealTimeSampleArrayMetricDescriptor.builder(), RealTimeSampleArrayMetricState.builder(), t -> {
                        t.withResolution(BigDecimal.ONE);
                        t.withMetricCategory(MetricCategory.UNSPEC);
                        t.withMetricAvailability(MetricAvailability.INTR);
                        t.withUnit(CodedValueFactory.createIeeeCodedValue("500"));
                        t.withSamplePeriod(Duration.ofSeconds(1));
                    }, t -> {}, Handles.CHANNEL_0))
                    .insert(entry(Handles.METRIC_4, DistributionSampleArrayMetricDescriptor.builder(), DistributionSampleArrayMetricState.builder(), t -> {
                        t.withResolution(BigDecimal.ONE);
                        t.withMetricCategory(MetricCategory.UNSPEC);
                        t.withMetricAvailability(MetricAvailability.INTR);
                        t.withUnit(CodedValueFactory.createIeeeCodedValue("500"));
                        t.withDomainUnit(CodedValueFactory.createIeeeCodedValue("1000"));
                        t.withDistributionRange(new Range());
                    }, t -> {}, Handles.CHANNEL_0))

                    .insert(entry(Handles.OPERATION_0, ActivateOperationDescriptor.builder(), ActivateOperationState.builder(), t -> {
                        t.withOperationTarget(Handles.MDS_0);
                    }, t -> {
                        t.withOperatingMode(OperatingMode.EN);
                    }, Handles.SCO_0))
                    .insert(entry(Handles.OPERATION_1, SetStringOperationDescriptor.builder(), SetStringOperationState.builder(), t -> {
                        t.withOperationTarget(Handles.MDS_0);
                    }, t -> {
                        t.withOperatingMode(OperatingMode.EN);
                    },Handles.SCO_0))
                    .insert(entry(Handles.OPERATION_2, SetValueOperationDescriptor.builder(), SetValueOperationState.builder(), t -> {
                        t.withOperationTarget(Handles.MDS_0);
                    }, t -> {
                        t.withOperatingMode(OperatingMode.EN);
                    },Handles.SCO_0))
                    .insert(entry(Handles.OPERATION_3, SetComponentStateOperationDescriptor.builder(), SetComponentStateOperationState.builder(), t -> {
                        t.withOperationTarget(Handles.MDS_0);
                    }, t -> {
                        t.withOperatingMode(OperatingMode.EN);
                    },Handles.SCO_0))
                    .insert(entry(Handles.OPERATION_4, SetMetricStateOperationDescriptor.builder(), SetMetricStateOperationState.builder(), t -> {
                        t.withOperationTarget(Handles.MDS_0);
                    }, t -> {
                        t.withOperatingMode(OperatingMode.EN);
                    },Handles.SCO_0))
                    .insert(entry(Handles.OPERATION_5, SetAlertStateOperationDescriptor.builder(), SetAlertStateOperationState.builder(), t -> {
                        t.withOperationTarget(Handles.MDS_0);
                    }, t -> {
                        t.withOperatingMode(OperatingMode.EN);
                    },Handles.SCO_0))
                    .insert(entry(Handles.OPERATION_6, SetContextStateOperationDescriptor.builder(), SetContextStateOperationState.builder(), t -> {
                        t.withOperationTarget(Handles.MDS_0);
                    }, t -> {
                        t.withOperatingMode(OperatingMode.EN);
                    },Handles.SCO_0))

                    .insert(contextEntry(Handles.CONTEXTDESCRIPTOR_0, Handles.CONTEXT_0, PatientContextDescriptor.builder(), PatientContextState.builder(), Handles.SYSTEMCONTEXT_0))
                    .insert(contextEntry(Handles.CONTEXTDESCRIPTOR_1, Handles.CONTEXT_1, LocationContextDescriptor.builder(), LocationContextState.builder(), Handles.SYSTEMCONTEXT_0))
                    .insert(contextEntry(Handles.CONTEXTDESCRIPTOR_2, Handles.CONTEXT_2, EnsembleContextDescriptor.builder(), EnsembleContextState.builder(), Handles.SYSTEMCONTEXT_0))
                    .insert(contextEntry(Handles.CONTEXTDESCRIPTOR_3, Handles.CONTEXT_3, WorkflowContextDescriptor.builder(), WorkflowContextState.builder(), Handles.SYSTEMCONTEXT_0))
                    .insert(contextEntry(Handles.CONTEXTDESCRIPTOR_4, Handles.CONTEXT_4, OperatorContextDescriptor.builder(), OperatorContextState.builder(), Handles.SYSTEMCONTEXT_0))
                    .insert(contextEntry(Handles.CONTEXTDESCRIPTOR_5, Handles.CONTEXT_5, MeansContextDescriptor.builder(), MeansContextState.builder(), Handles.SYSTEMCONTEXT_0));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // includes all entities plus data plus exemplary extensions
    public MdibDescriptionModifications createFullyPopulatedTree() {
        return MdibDescriptionModifications.create()
                .insert(dataGenerator.mdsDescriptor(Handles.MDS_0), dataGenerator.mdsState())
                .insert(dataGenerator.clockDescriptor(Handles.CLOCK_0), dataGenerator.clockState(), Handles.MDS_0)
                .insert(dataGenerator.batteryDescriptor(Handles.BATTERY_0), dataGenerator.batteryState(), Handles.MDS_0)
                .insert(dataGenerator.systemContextDescriptor(Handles.SYSTEMCONTEXT_0), dataGenerator.systemContextState(), Handles.MDS_0)
                .insert(dataGenerator.patientContextDescriptor(Handles.CONTEXTDESCRIPTOR_0), Arrays.asList(dataGenerator.patientContextState(Handles.CONTEXT_0)), Handles.SYSTEMCONTEXT_0)
                .insert(dataGenerator.locationContextDescriptor(Handles.CONTEXTDESCRIPTOR_1), Arrays.asList(dataGenerator.locationContextState(Handles.CONTEXT_1)), Handles.SYSTEMCONTEXT_0)
                .insert(dataGenerator.ensembleContextDescriptor(Handles.CONTEXTDESCRIPTOR_3), Arrays.asList(dataGenerator.ensembleContextState(Handles.CONTEXT_3)), Handles.SYSTEMCONTEXT_0)
                .insert(dataGenerator.alertSystemDescriptor(Handles.ALERTSYSTEM_0), dataGenerator.alertSystemState(), Handles.MDS_0)
                .insert(dataGenerator.alertConditionDescriptor(Handles.ALERTCONDITION_0, Handles.MDS_0), dataGenerator.alertConditionState(), Handles.ALERTSYSTEM_0)
                .insert(dataGenerator.limitAlertConditionDescriptor(Handles.ALERTCONDITION_1, Handles.MDS_0), dataGenerator.limitAlertConditionState(), Handles.ALERTSYSTEM_0)
                .insert(dataGenerator.alertSignalDescriptor(Handles.ALERTSIGNAL_0, Handles.ALERTCONDITION_0, AlertSignalManifestation.VIS), dataGenerator.alertSignalState(), Handles.ALERTSYSTEM_0)
                .insert(dataGenerator.alertSignalDescriptor(Handles.ALERTSIGNAL_1, Handles.ALERTCONDITION_0, AlertSignalManifestation.AUD), dataGenerator.alertSignalState(), Handles.ALERTSYSTEM_0)
                .insert(dataGenerator.alertSignalDescriptor(Handles.ALERTSIGNAL_2, Handles.ALERTCONDITION_1, AlertSignalManifestation.VIS), dataGenerator.alertSignalState(), Handles.ALERTSYSTEM_0)
                .insert(dataGenerator.alertSignalDescriptor(Handles.ALERTSIGNAL_3, Handles.ALERTCONDITION_1, AlertSignalManifestation.AUD), dataGenerator.alertSignalState(), Handles.ALERTSYSTEM_0)
                .insert(dataGenerator.vmdDescriptor(Handles.VMD_0), dataGenerator.vmdState(), Handles.MDS_0)
                .insert(dataGenerator.alertSystemDescriptor(Handles.ALERTSYSTEM_1), dataGenerator.alertSystemState(), Handles.MDS_0)
                .insert(dataGenerator.alertConditionDescriptor(Handles.ALERTCONDITION_2, Handles.MDS_0), dataGenerator.alertConditionState(), Handles.ALERTSYSTEM_0)
                .insert(dataGenerator.limitAlertConditionDescriptor(Handles.ALERTCONDITION_3, Handles.MDS_0), dataGenerator.limitAlertConditionState(), Handles.ALERTSYSTEM_0)
                .insert(dataGenerator.alertSignalDescriptor(Handles.ALERTSIGNAL_4, Handles.ALERTCONDITION_2, AlertSignalManifestation.VIS), dataGenerator.alertSignalState(), Handles.ALERTSYSTEM_1)
                .insert(dataGenerator.alertSignalDescriptor(Handles.ALERTSIGNAL_5, Handles.ALERTCONDITION_2, AlertSignalManifestation.AUD), dataGenerator.alertSignalState(), Handles.ALERTSYSTEM_1)
                .insert(dataGenerator.alertSignalDescriptor(Handles.ALERTSIGNAL_6, Handles.ALERTCONDITION_3, AlertSignalManifestation.VIS), dataGenerator.alertSignalState(), Handles.ALERTSYSTEM_1)
                .insert(dataGenerator.alertSignalDescriptor(Handles.ALERTSIGNAL_7, Handles.ALERTCONDITION_3, AlertSignalManifestation.AUD), dataGenerator.alertSignalState(), Handles.ALERTSYSTEM_1)
                .insert(dataGenerator.channelDescriptor(Handles.CHANNEL_0), dataGenerator.channelState(), Handles.VMD_0)
                .insert(dataGenerator.numericMetricDescriptor(Handles.METRIC_0), dataGenerator.numericMetricState(), Handles.CHANNEL_0)
                .insert(dataGenerator.stringMetricDescriptor(Handles.METRIC_1), dataGenerator.stringMetricState(), Handles.CHANNEL_0)
                .insert(dataGenerator.enumStringMetricDescriptor(Handles.METRIC_2), dataGenerator.enumStringMetricState(), Handles.CHANNEL_0)
                .insert(dataGenerator.realTimeSampleArrayMetricDescriptor(Handles.METRIC_3), dataGenerator.realTimeSampleArrayMetricState(), Handles.CHANNEL_0)
                .insert(dataGenerator.distributionSampleArrayMetricDescriptor(Handles.METRIC_4), dataGenerator.distributionSampleArrayMetricState(), Handles.CHANNEL_0)
                .insert(dataGenerator.scoDescriptor(Handles.SCO_0), dataGenerator.scoState(), Handles.MDS_0)
                .insert(dataGenerator.activateOperationDescriptor(Handles.OPERATION_0, Handles.MDS_0), dataGenerator.activateOperationState(), Handles.SCO_0)
                .insert(dataGenerator.setStringOperationDescriptor(Handles.OPERATION_1, Handles.METRIC_1), dataGenerator.setStringOperationState(), Handles.SCO_0)
                .insert(dataGenerator.setValueOperationDescriptor(Handles.OPERATION_2, Handles.METRIC_0), dataGenerator.setValueOperationState(), Handles.SCO_0)
                .insert(dataGenerator.setComponentStateOperationDescriptor(Handles.OPERATION_3, Handles.MDS_0), dataGenerator.setComponentStateOperationState(), Handles.SCO_0)
                .insert(dataGenerator.setMetricStateOperationDescriptor(Handles.OPERATION_4, Handles.METRIC_2), dataGenerator.setMetricStateOperationState(), Handles.SCO_0)
                .insert(dataGenerator.setAlertStateOperationDescriptor(Handles.OPERATION_5, Handles.ALERTCONDITION_0), dataGenerator.setAlertStateOperationState(), Handles.SCO_0)
                .insert(dataGenerator.setContextStateOperationDescriptor(Handles.OPERATION_6, Handles.CONTEXTDESCRIPTOR_0), dataGenerator.setContextStateOperationState(), Handles.SCO_0)
                .insert(dataGenerator.scoDescriptor(Handles.SCO_1), dataGenerator.scoState(), Handles.VMD_0)
                .insert(dataGenerator.activateOperationDescriptor(Handles.OPERATION_7, Handles.VMD_0), dataGenerator.activateOperationState(), Handles.SCO_1)
                .insert(dataGenerator.setStringOperationDescriptor(Handles.OPERATION_8, Handles.METRIC_1), dataGenerator.setStringOperationState(), Handles.SCO_1)
                .insert(dataGenerator.setValueOperationDescriptor(Handles.OPERATION_9, Handles.METRIC_0), dataGenerator.setValueOperationState(), Handles.SCO_1)
                .insert(dataGenerator.setComponentStateOperationDescriptor(Handles.OPERATION_10, Handles.VMD_0), dataGenerator.setComponentStateOperationState(), Handles.SCO_1)
                .insert(dataGenerator.setMetricStateOperationDescriptor(Handles.OPERATION_11, Handles.METRIC_2), dataGenerator.setMetricStateOperationState(), Handles.SCO_1)
                .insert(dataGenerator.setAlertStateOperationDescriptor(Handles.OPERATION_12, Handles.ALERTSIGNAL_7), dataGenerator.setAlertStateOperationState(), Handles.SCO_1);
    }

    private <T extends AbstractDescriptor.Builder<?>, V extends AbstractState.Builder<?>> MdibDescriptionModifications.Entry entry(
        String handle, T descrClass, V stateClass) throws Exception {
        return entry(handle, descrClass, stateClass, t -> {}, t -> {}, null);
    }

    private <T extends AbstractDescriptor.Builder<?>, V extends AbstractState.Builder<?>> MdibDescriptionModifications.Entry entry(
        String handle, Consumer<T> descrConsumer, Consumer<V> stateConsumer, T descrClass, V stateClass) throws Exception {
        return entry(handle, descrClass, stateClass, descrConsumer, stateConsumer, null);
    }

    private <T extends AbstractDescriptor.Builder<?>, V extends AbstractState.Builder<?>> MdibDescriptionModifications.Entry entry(
        String handle, T descrClass, V stateClass, @Nullable String parentHandle) throws Exception {
        return entryFactory.entry(handle, descrClass, stateClass, t -> {}, t -> {}, parentHandle);
    }

    private <T extends AbstractDescriptor.Builder<?>, V extends AbstractState.Builder<?>> MdibDescriptionModifications.Entry entry(String handle,
                                                                                                             T descrClass,
                                                                                                             V stateClass,
                                                                                                             Consumer<T> descrConsumer,
                                                                                                             Consumer<V> stateConsumer,
                                                                                                             @Nullable String parentHandle) throws Exception {
        return entryFactory.entry(handle, descrClass, stateClass, descrConsumer, stateConsumer, parentHandle);
    }

    private <T extends AbstractDescriptor.Builder<?>, V extends AbstractContextState.Builder<?>> MdibDescriptionModifications.MultiStateEntry contextEntry(String handle,
                                                                                                                                     String stateHandle,
                                                                                                                                     T descrClass,
                                                                                                                                     V stateClass,
                                                                                                                                     String parentHandle) throws Exception {
        return entryFactory.contextEntry(handle, Collections.singletonList(stateHandle), descrClass, stateClass, parentHandle);
    }

    private <T extends AbstractDescriptor.Builder<?>> T descriptor(String handle, T theClass) {
        return MockModelFactory.createDescriptor(handle, theClass);
    }

    private <T extends AbstractState.Builder<?>> T state(String handle, T theClass) {
        return MockModelFactory.createState(handle, theClass);
    }
}
