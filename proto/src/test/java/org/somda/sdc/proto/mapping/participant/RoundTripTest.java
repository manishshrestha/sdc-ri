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
        resultsToCompare.add(new PatientContextStateRoundTrip(modifications));
        resultsToCompare.add(new LimitAlertConditionRoundTrip(modifications));
    }

    /**
     * List of already (fully, excluding extensions) covered participant model types
     * with at least round trip using them.
     *
     * AbstractAlertDescriptor ({@linkplain LimitAlertConditionRoundTrip})
     * AbstractAlertState ({@linkplain LimitAlertConditionRoundTrip})
     * AbstractDescriptor ({@linkplain LimitAlertConditionRoundTrip})
     * AbstractState ({@linkplain LimitAlertConditionRoundTrip})
     * ActivateOperationDescriptor ({@linkplain ActivateOperationRoundTrip})
     * ActivateOperationState ({@linkplain ActivateOperationRoundTrip})
     * AlertConditionDescriptor ({@linkplain LimitAlertConditionRoundTrip}, {@linkplain AlertConditionRoundTrip})
     * AlertConditionState ({@linkplain LimitAlertConditionRoundTrip}, {@linkplain AlertConditionRoundTrip})
     * AlertSignalDescriptor ({@linkplain AlertSignalRoundTrip})
     * AlertSignalState ({@linkplain AlertSignalRoundTrip})
     * EnumStringMetricDescriptor ({@linkplain EnumStringMetricRoundTrip})
     * EnumStringMetricState ({@linkplain EnumStringMetricRoundTrip})
     * LimitAlertConditionDescriptor ({@linkplain LimitAlertConditionRoundTrip})
     * LimitAlertConditionState ({@linkplain LimitAlertConditionRoundTrip})
     * Range ({@linkplain LimitAlertConditionRoundTrip})
     *
     */
    private static void covered() {
        // holder for javadoc
    }

    /**
     * List of not yet covered participant model types.
     *
     * AbstractComplexDeviceComponentDescriptor
     * AbstractComplexDeviceComponentState
     * AbstractContextDescriptor
     * AbstractContextState
     * AbstractDeviceComponentDescriptor
     * AbstractDeviceComponentState
     * AbstractMetricDescriptor
     * AbstractMetricState
     * AbstractMetricValue
     * AbstractMultiState
     * AbstractOperationDescriptor
     * AbstractOperationState
     * AbstractSetStateOperationDescriptor
     * AlertSystemDescriptor
     * AlertSystemState
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
     * EnsembleContextDescriptor
     * EnsembleContextState
     * ImagingProcedure
     * InstanceIdentifier
     * LocalizedText
     * LocationContextDescriptor
     * LocationContextState
     * LocationDetail
     * LocationReference
     * MdDescription
     * Mdib
     * MdsDescriptor
     * MdsState
     * MdState
     * MeansContextDescriptor
     * MeansContextState
     * Measurement
     * NeonatalPatientDemographicsCoreData
     * NumericMetricDescriptor
     * NumericMetricState
     * NumericMetricValue
     * OperatingJurisdiction
     * OperatorContextDescriptor
     * OperatorContextState
     * OrderDetail
     * PatientContextDescriptor
     * PatientContextState
     * PatientDemographicsCoreData
     * PersonParticipation
     * PersonReference
     * PhysicalConnectorInfo
     * RealTimeSampleArrayMetricDescriptor
     * RealTimeSampleArrayMetricState
     * RemedyInfo
     * SampleArrayValue
     * ScoDescriptor
     * ScoState
     * SetAlertStateOperationDescriptor
     * SetAlertStateOperationState
     * SetComponentStateOperationDescriptor
     * SetComponentStateOperationState
     * SetContextStateOperationDescriptor
     * SetContextStateOperationState
     * SetMetricStateOperationDescriptor
     * SetMetricStateOperationState
     * SetStringOperationDescriptor
     * SetStringOperationState
     * SetValueOperationDescriptor
     * SetValueOperationState
     * StringMetricDescriptor
     * StringMetricState
     * StringMetricValue
     * SystemContextDescriptor
     * SystemContextState
     * SystemSignalActivation
     * VmdDescriptor
     * VmdState
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
