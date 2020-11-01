package org.somda.sdc.proto.mapping.participant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.consumer.access.factory.RemoteMdibAccessFactory;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.provider.access.factory.LocalMdibAccessFactory;
import org.somda.sdc.proto.UnitTestUtil;
import org.somda.sdc.proto.mapping.participant.factory.PojoToProtoTreeMapperFactory;
import org.somda.sdc.proto.mapping.participant.factory.ProtoToPojoModificationsBuilderFactory;
import test.org.somda.common.LoggingTestWatcher;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.function.BiConsumer;

@ExtendWith(LoggingTestWatcher.class)
class RoundTripTest {
    private static final UnitTestUtil UT = new UnitTestUtil();

    LocalMdibAccess mdibAccessSource;
    private PojoToProtoTreeMapper pojoToProtoMapper;
    private RemoteMdibAccess mdibAccessSink;
    private ProtoToPojoModificationsBuilderFactory protoToPojoMapperFactory;

    @BeforeEach
    void beforeEach() {
        mdibAccessSource = UT.getInjector().getInstance(LocalMdibAccessFactory.class).createLocalMdibAccess();
        pojoToProtoMapper = UT.getInjector().getInstance(PojoToProtoTreeMapperFactory.class).create(mdibAccessSource);
        mdibAccessSink = UT.getInjector().getInstance(RemoteMdibAccessFactory.class).createRemoteMdibAccess();
        protoToPojoMapperFactory = UT.getInjector().getInstance(ProtoToPojoModificationsBuilderFactory.class);
    }

    @Test
    @DisplayName("Full MDIB")
    void mapMdib() throws Exception {
        var modifications = MdibDescriptionModifications.create();
        var resultsToCompare = new ArrayList<BiConsumer<LocalMdibAccess, RemoteMdibAccess>>();

        attachTreeTests(modifications, resultsToCompare);

        mdibAccessSource.writeDescription(modifications);
        var mdibMsg = pojoToProtoMapper.mapMdib();
        var builder = protoToPojoMapperFactory.create(mdibMsg);
        mdibAccessSink.writeDescription(MdibVersion.create(), BigInteger.ZERO, BigInteger.ZERO, builder.get());

        for (var consumer : resultsToCompare) {
            consumer.accept(mdibAccessSource, mdibAccessSink);
        }
    }

    private void attachTreeTests(MdibDescriptionModifications modifications,
                                 ArrayList<BiConsumer<LocalMdibAccess, RemoteMdibAccess>> resultsToCompare) {
        resultsToCompare.add(new MdsRoundTrip(modifications));
        resultsToCompare.add(new VmdRoundTrip(modifications));
        resultsToCompare.add(new ChannelRoundTrip(modifications));
        resultsToCompare.add(new StringMetricRoundTrip(modifications));
        resultsToCompare.add(new EnumStringMetricRoundTrip(modifications));
        resultsToCompare.add(new NumericMetricRoundTrip(modifications));
        resultsToCompare.add(new RealTimeDistributionSampleArrayRoundTrip(modifications));
        resultsToCompare.add(new SystemContextRoundTrip(modifications));
        resultsToCompare.add(new EnsembleContextRoundTrip(modifications));
        resultsToCompare.add(new LocationContextRoundTrip(modifications));
        resultsToCompare.add(new AlertSystemRoundTrip(modifications));
        resultsToCompare.add(new ScoRoundTrip(modifications));
        resultsToCompare.add(new AlertConditionRoundTrip(modifications));
        resultsToCompare.add(new AlertSignalRoundTrip(modifications));
        resultsToCompare.add(new SetMetricStateOperationRoundTrip(modifications));
        resultsToCompare.add(new SetComponentStateOperationRoundTrip(modifications));
        resultsToCompare.add(new SetContextStateOperationRoundTrip(modifications));
        resultsToCompare.add(new SetAlertStateOperationRoundTrip(modifications));
        resultsToCompare.add(new ActivateOperationRoundTrip(modifications));
        resultsToCompare.add(new SetStringOperationRoundTrip(modifications));
        resultsToCompare.add(new SetValueOperationRoundTrip(modifications));
        resultsToCompare.add(new PatientContextRoundTrip(modifications));
        resultsToCompare.add(new LimitAlertConditionRoundTrip(modifications));
    }

    /**
     * List of already (fully, excluding extensions) covered participant model types
     * with at least round trip using them.
     *
     * AbstractAlertDescriptor ({@linkplain LimitAlertConditionRoundTrip})
     * AbstractAlertState ({@linkplain LimitAlertConditionRoundTrip})
     * AbstractComplexDeviceComponentDescriptor ({@linkplain MdsRoundTrip}, {@linkplain VmdRoundTrip})
     * AbstractComplexDeviceComponentState ({@linkplain MdsRoundTrip}, {@linkplain VmdRoundTrip})
     * AbstractContextDescriptor ({@linkplain EnumStringMetricRoundTrip}, {@linkplain PatientContextRoundTrip})
     * AbstractContextState ({@linkplain EnumStringMetricRoundTrip}, {@linkplain PatientContextRoundTrip})
     * AbstractDescriptor ({@linkplain LimitAlertConditionRoundTrip})
     * AbstractState ({@linkplain LimitAlertConditionRoundTrip})
     * ActivateOperationDescriptor ({@linkplain ActivateOperationRoundTrip})
     * ActivateOperationState ({@linkplain ActivateOperationRoundTrip})
     * AlertConditionDescriptor ({@linkplain LimitAlertConditionRoundTrip}, {@linkplain AlertConditionRoundTrip})
     * AlertConditionState ({@linkplain LimitAlertConditionRoundTrip}, {@linkplain AlertConditionRoundTrip})
     * AlertSignalDescriptor ({@linkplain AlertSignalRoundTrip})
     * AlertSignalState ({@linkplain AlertSignalRoundTrip})
     * AlertSystemDescriptor ({@linkplain AlertSystemRoundTrip})
     * AlertSystemState ({@linkplain AlertSystemRoundTrip})
     * EnsembleContextDescriptor ({@linkplain EnsembleContextRoundTrip})
     * EnsembleContextState ({@linkplain EnsembleContextRoundTrip})
     * EnumStringMetricDescriptor ({@linkplain EnumStringMetricRoundTrip})
     * EnumStringMetricState ({@linkplain EnumStringMetricRoundTrip})
     * LimitAlertConditionDescriptor ({@linkplain LimitAlertConditionRoundTrip})
     * LimitAlertConditionState ({@linkplain LimitAlertConditionRoundTrip})
     * LocationContextDescriptor ({@linkplain LocationContextRoundTrip})
     * LocationContextState ({@linkplain LocationContextRoundTrip})
     * MdsDescriptor ({@linkplain MdsRoundTrip})
     * MdsState ({@linkplain MdsRoundTrip})
     * NumericMetricDescriptor ({@linkplain NumericMetricRoundTrip})
     * NumericMetricState ({@linkplain NumericMetricRoundTrip})
     * NumericMetricValue ({@linkplain NumericMetricRoundTrip})
     * PatientContextDescriptor ({@linkplain PatientContextRoundTrip})
     * PatientContextState ({@linkplain PatientContextRoundTrip})
     * Range ({@linkplain LimitAlertConditionRoundTrip})
     * RealTimeSampleArrayMetricDescriptor ({@linkplain RealTimeDistributionSampleArrayRoundTrip})
     * RealTimeSampleArrayMetricState ({@linkplain RealTimeDistributionSampleArrayRoundTrip})
     * SampleArrayValue ({@linkplain RealTimeDistributionSampleArrayRoundTrip})
     * ScoDescriptor ({@linkplain ScoRoundTrip})
     * ScoState ({@linkplain ScoRoundTrip})
     * SetAlertStateOperationDescriptor ({@linkplain SetAlertStateOperationRoundTrip})
     * SetAlertStateOperationState ({@linkplain SetAlertStateOperationRoundTrip})
     * SetComponentStateOperationDescriptor ({@linkplain SetComponentStateOperationRoundTrip})
     * SetComponentStateOperationState  ({@linkplain SetComponentStateOperationRoundTrip})
     * SetContextStateOperationDescriptor  ({@linkplain SetContextStateOperationRoundTrip})
     * SetContextStateOperationState  ({@linkplain SetContextStateOperationRoundTrip})
     * SetMetricStateOperationDescriptor  ({@linkplain SetMetricStateOperationRoundTrip})
     * SetMetricStateOperationState  ({@linkplain SetMetricStateOperationRoundTrip})
     * SetStringOperationDescriptor ({@linkplain SetStringOperationRoundTrip})
     * SetStringOperationState ({@linkplain SetStringOperationRoundTrip})
     * SetValueOperationDescriptor ({@linkplain SetValueOperationRoundTrip})
     * SetValueOperationState ({@linkplain SetValueOperationRoundTrip})
     * StringMetricDescriptor ({@linkplain StringMetricRoundTrip})
     * StringMetricState ({@linkplain StringMetricRoundTrip})
     * StringMetricValue ({@linkplain StringMetricRoundTrip})
     * SystemContextDescriptor ({@linkplain SystemContextRoundTrip})
     * SystemContextState ({@linkplain SystemContextRoundTrip})
     * VmdDescriptor ({@linkplain VmdRoundTrip})
     * VmdState ({@linkplain VmdRoundTrip})
     *
     */
    private static void covered() {
        // holder for javadoc
    }

    /**
     * List of not yet fully covered participant model types.
     *
     * AbstractDeviceComponentDescriptor
     * AbstractDeviceComponentState
     * AbstractMetricDescriptor
     * AbstractMetricState
     * AbstractMetricValue
     * AbstractMultiState
     * AbstractOperationDescriptor
     * AbstractOperationState
     * AbstractSetStateOperationDescriptor
     * ApprovedJurisdictions
     * BaseDemographics
     * BatteryDescriptor
     * BatteryState
     * CalibrationInfo
     * CauseInfo
     * ChannelDescriptor
     * ChannelState
     * ClinicalInfo
     * ClockDescriptor
     * ClockState
     * CodedValue
     * ContainmentTree
     * ContainmentTreeEntry
     * DistributionSampleArrayMetricDescriptor
     * DistributionSampleArrayMetricState
     * ImagingProcedure
     * InstanceIdentifier
     * LocalizedText
     * LocationDetail
     * LocationReference
     * MdDescription
     * Mdib
     * MdState
     * MeansContextDescriptor
     * MeansContextState
     * Measurement
     * NeonatalPatientDemographicsCoreData
     * OperatingJurisdiction
     * OperatorContextDescriptor
     * OperatorContextState
     * OrderDetail
     * PatientDemographicsCoreData
     * PersonParticipation
     * PersonReference
     * PhysicalConnectorInfo
     * RemedyInfo
     * SystemSignalActivation
     * WorkflowContextDescriptor
     * WorkflowContextState
     *
     * AlertActivation
     * AlertConditionKind
     * AlertConditionMonitoredLimits
     * AlertConditionPriority
     * AlertConditionReference
     * AlertSignalManifestation
     * AlertSignalPresence
     * AlertSignalPrimaryLocation
     * CalibrationState
     * CalibrationType
     * CodeIdentifier
     * ComponentActivation
     * ContextAssociation
     * DerivationMethod
     * EntryRef
     * GenerationMode
     * Handle
     * HandleRef
     * LocalizedTextContent
     * LocalizedTextRef
     * LocalizedTextWidth
     * MdsOperatingMode
     * MeasurementValidity
     * MetricAvailability
     * MetricCategory
     * OperatingMode
     * OperationRef
     * PatientType
     * QualityIndicator
     * RealTimeValueType
     * ReferencedVersion
     * SafetyClassification
     * Sex
     * SymbolicCodeName
     * Timestamp
     * TimeZone
     * VersionCounter
     *
     * ContainmentTreeInfo
     * MdibVersionGroup
     */
    static void uncovered() {
        // holder for javadoc
    }
}
