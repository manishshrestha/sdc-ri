package org.somda.sdc.biceps.testutil;

import org.somda.sdc.biceps.model.extension.ExtensionType;
import org.somda.sdc.biceps.model.message.RetrievabilityMethod;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.common.util.AnyDateTime;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public class DescriptorStateDataGenerator {
    private final BaseTypeDataGenerator baseTypes;

    public DescriptorStateDataGenerator() {
        this.baseTypes = new BaseTypeDataGenerator();
    }

    public MdsDescriptor mdsDescriptor(String handle) {
        final var mdsDescriptorBuilder = MdsDescriptor.builder();
        descriptor(mdsDescriptorBuilder, handle);
        deviceComponentDescriptor(mdsDescriptorBuilder);

        mdsDescriptorBuilder.withApprovedJurisdictions(baseTypes.approvedJurisdictions());
        final var mdsDescriptorMetaDataBuilder = MdsDescriptor.MetaData.builder()
            .withExpirationDate(AnyDateTime.create(baseTypes.localDateTime()))
            .withLotNumber("lot-number")
            .withManufactureDate(AnyDateTime.create(baseTypes.localDateTime()))
            .withManufacturer(baseTypes.localizedTexts())
            .withModelName(baseTypes.localizedTexts())
            .withModelNumber("model-number")
            .withSerialNumber(Arrays.asList("serial-number1", "serial-number2"));
        final var udiBuilder = MdsDescriptor.MetaData.Udi.builder()
            .withDeviceIdentifier("udi-device-identifier")
            .withHumanReadableForm("udi-human-readable-form")
            .withJurisdiction(baseTypes.instanceIdentifier("udi-jurisdiction"))
            .withIssuer(baseTypes.instanceIdentifier("udi-issuer"));
        mdsDescriptorMetaDataBuilder.withUdi(List.of(udiBuilder.build()));

        return mdsDescriptorBuilder.build();
    }

    public MdsState mdsState() {
        final var mdsState = MdsState.builder()
            .withLang("en")
            .withOperatingJurisdiction(baseTypes.operatingJurisdiction("operating-jurisdiction"))
            .withOperatingMode(MdsOperatingMode.DMO);
        deviceComponentState(mdsState);

        return mdsState.build();
    }

    public ClockDescriptor clockDescriptor(String handle) {
        final var clockDescriptorBuilder = ClockDescriptor.builder();
        descriptor(clockDescriptorBuilder, handle);
        deviceComponentDescriptor(clockDescriptorBuilder);
        clockDescriptorBuilder
            .withResolution(Duration.ofMillis(1))
            .withTimeProtocol(Arrays.asList(baseTypes.codedValue("time-protocol1"), baseTypes.codedValue("time-protocol2")));
        return clockDescriptorBuilder.build();
    }

    public ClockState clockState() {
        final var clockStateBuilder = ClockState.builder();
        deviceComponentState(clockStateBuilder);
        clockStateBuilder.withCriticalUse(false)
            .withActiveSyncProtocol(baseTypes.codedValue("time-protocol1"))
            .withAccuracy(BigDecimal.ONE)
            .withDateAndTime(Instant.ofEpochMilli(1580152377910L))
            .withLastSet(Instant.ofEpochMilli(1580152377910L).minus(Duration.ofHours(5)))
            .withReferenceSource(List.of("0.de.pool.ntp.org"))
            .withRemoteSync(true)
            .withTimeZone("CST6CDT,M3.2.0/2:00:00,M11.1.0/2:00:00");
        return clockStateBuilder.build();
    }

    public BatteryDescriptor batteryDescriptor(String handle) {
        final var batteryDescriptorBuilder = BatteryDescriptor.builder();
        descriptor(batteryDescriptorBuilder, handle);
        deviceComponentDescriptor(batteryDescriptorBuilder);
        batteryDescriptorBuilder.withCapacityFullCharge(baseTypes.measurement(BigDecimal.valueOf(100)))
            .withCapacitySpecified(baseTypes.measurement(BigDecimal.valueOf(120)))
            .withVoltageSpecified(baseTypes.measurement(BigDecimal.TEN));
        return batteryDescriptorBuilder.build();
    }

    public BatteryState batteryState() {
        final var batteryStateBuilder = BatteryState.builder();
        deviceComponentState(batteryStateBuilder);
        batteryStateBuilder.withCapacityRemaining(baseTypes.measurement(BigDecimal.valueOf(80)))
            .withChargeCycles(100L)
            .withChargeStatus(BatteryState.ChargeStatus.CH_B)
            .withCurrent(baseTypes.measurement(BigDecimal.TEN))
            .withTemperature(baseTypes.measurement(BigDecimal.valueOf(45)))
            .withVoltage(baseTypes.measurement(BigDecimal.valueOf(8)))
            .withRemainingBatteryTime(baseTypes.measurement(BigDecimal.ONE));
        return batteryStateBuilder.build();
    }

    public SystemContextDescriptor systemContextDescriptor(String handle) {
        final var systemContextDescriptor = SystemContextDescriptor.builder();
        descriptor(systemContextDescriptor, handle);
        deviceComponentDescriptor(systemContextDescriptor);
        return systemContextDescriptor.build();
    }

    public SystemContextState systemContextState() {
        final var systemContextState = SystemContextState.builder();
        deviceComponentState(systemContextState);
        return systemContextState.build();
    }

    public PatientContextDescriptor patientContextDescriptor(String handle) {
        final var patientContextDescriptor = PatientContextDescriptor.builder();
        descriptor(patientContextDescriptor, handle);
        return patientContextDescriptor.build();
    }

    public PatientContextState patientContextState(String handle) {
        final var patientContextState = PatientContextState.builder();
        contextState(patientContextState, handle);
        return patientContextState.withCoreData(baseTypes.patientDemographicsCoreData())
            .build();
    }

    public LocationContextDescriptor locationContextDescriptor(String handle) {
        final var locationContextDescriptor = LocationContextDescriptor.builder();
        descriptor(locationContextDescriptor, handle);
        return locationContextDescriptor.build();
    }

    public LocationContextState locationContextState(String handle) {
        final var locationContextState = LocationContextState.builder();
        contextState(locationContextState, handle);
        return locationContextState.withLocationDetail(baseTypes.locationDetail()).build();
    }

    public EnsembleContextDescriptor ensembleContextDescriptor(String handle) {
        final var ensembleContextDescriptor = EnsembleContextDescriptor.builder();
        descriptor(ensembleContextDescriptor, handle);
        return ensembleContextDescriptor.build();
    }

    public EnsembleContextState ensembleContextState(String handle) {
        final var ensembleContextState = EnsembleContextState.builder();
        contextState(ensembleContextState, handle);
        return ensembleContextState.build();
    }

    public AlertSystemDescriptor alertSystemDescriptor(String handle) {
        final var alertSystemDescriptor = AlertSystemDescriptor.builder();
        descriptor(alertSystemDescriptor, handle);
        alertSystemDescriptor.withMaxPhysiologicalParallelAlarms(10L)
            .withMaxTechnicalParallelAlarms(5L)
            .withSelfCheckPeriod(Duration.ofMillis(5000));
        return alertSystemDescriptor.build();
    }

    public AlertSystemState alertSystemState() {
        final var alertSystemState = AlertSystemState.builder();
        alertState(alertSystemState);
        alertSystemState.withLastSelfCheck(Instant.ofEpochMilli(1580152377910L).minus(Duration.ofHours(2)))
            .withSelfCheckCount(1234L)
            .withSystemSignalActivation(
                Arrays.asList(baseTypes.systemSignalActivation(AlertSignalManifestation.AUD),
                baseTypes.systemSignalActivation(AlertSignalManifestation.VIS))
            );
        return alertSystemState.build();
    }

    public AlertConditionDescriptor alertConditionDescriptor(String handle, String sourceHandle) {
        final var alertConditionDescriptor = AlertConditionDescriptor.builder();
        descriptor(alertConditionDescriptor, handle);
        alertConditionDescriptor.withCanDeescalate(AlertConditionPriority.NONE)
            .withCanEscalate(AlertConditionPriority.HI)
            .withCauseInfo(List.of(baseTypes.causeInfo()))
            .withDefaultConditionGenerationDelay(Duration.ofMillis(100))
            .withKind(AlertConditionKind.TEC)
            .withPriority(AlertConditionPriority.ME)
            .withSource(List.of(sourceHandle));
        return alertConditionDescriptor.build();
    }

    public AlertConditionState alertConditionState() {
        final var alertConditionState = AlertConditionState.builder();
        alertState(alertConditionState);
        alertConditionState.withActualConditionGenerationDelay(Duration.ofMillis(50))
            .withActualPriority(AlertConditionPriority.ME)
            .withDeterminationTime(Instant.ofEpochMilli(1580152377910L))
            .withPresence(false)
            .withRank(5);
        return alertConditionState.build();
    }

    public LimitAlertConditionDescriptor limitAlertConditionDescriptor(String handle, String sourceHandle) {
        final var limitAlertConditionDescriptor = LimitAlertConditionDescriptor.builder();
        descriptor(limitAlertConditionDescriptor, handle);
        limitAlertConditionDescriptor.withCanDeescalate(AlertConditionPriority.ME)
            .withCanEscalate(AlertConditionPriority.HI)
            .withCauseInfo(List.of(baseTypes.causeInfo()))
            .withDefaultConditionGenerationDelay(Duration.ofMillis(10))
            .withKind(AlertConditionKind.PHY)
            .withPriority(AlertConditionPriority.HI)
            .withSource(List.of(sourceHandle))
            .withAutoLimitSupported(false)
            .withMaxLimits(baseTypes.range());
        return limitAlertConditionDescriptor.build();
    }

    public LimitAlertConditionState limitAlertConditionState() {
        final var limitAlertConditionState = LimitAlertConditionState.builder();
        alertState(limitAlertConditionState);
        limitAlertConditionState.withActualConditionGenerationDelay(Duration.ofMillis(10))
            .withActualPriority(AlertConditionPriority.NONE)
            .withDeterminationTime(Instant.ofEpochMilli(1580152377910L))
            .withPresence(false)
            .withRank(3)
            .withAutoLimitActivationState(AlertActivation.PSD)
            .withLimits(baseTypes.range())
            .withMonitoredAlertLimits(AlertConditionMonitoredLimits.ALL);
        return limitAlertConditionState.build();
    }

    public AlertSignalDescriptor alertSignalDescriptor(String handle, String conditionSignaledHandle, AlertSignalManifestation manifestation) {
        final var alertSignalDescriptor = AlertSignalDescriptor.builder();
        descriptor(alertSignalDescriptor, handle);
        alertSignalDescriptor.withAcknowledgementSupported(false)
            .withAcknowledgeTimeout(Duration.ofMillis(1000))
            .withConditionSignaled(conditionSignaledHandle)
            .withDefaultSignalGenerationDelay(Duration.ofMillis(500))
            .withLatching(true)
            .withManifestation(manifestation)
            .withMaxSignalGenerationDelay(Duration.ofMillis(100))
            .withMinSignalGenerationDelay(Duration.ofMillis(10))
            .withSignalDelegationSupported(false);
        return alertSignalDescriptor.build();
    }

    public AlertSignalState alertSignalState() {
        final var alertSignalState = AlertSignalState.builder()
            .withActualSignalGenerationDelay(Duration.ofMillis(30))
            .withLocation(AlertSignalPrimaryLocation.LOC)
            .withPresence(AlertSignalPresence.OFF)
            .withSlot(2L)
            .withActivationState(AlertActivation.OFF);
        return alertSignalState.build();
    }

    public VmdDescriptor vmdDescriptor(String handle) {
        final var vmdDescriptor = VmdDescriptor.builder();
        descriptor(vmdDescriptor, handle);
        deviceComponentDescriptor(vmdDescriptor);
        vmdDescriptor.withApprovedJurisdictions(baseTypes.approvedJurisdictions());
        return vmdDescriptor.build();
    }

    public VmdState vmdState() {
        final var vmdState = VmdState.builder();
        deviceComponentState(vmdState);
        vmdState.withOperatingJurisdiction(baseTypes.operatingJurisdiction("operating-jurisdiction"));
        return vmdState.build();
    }

    public ChannelDescriptor channelDescriptor(String handle) {
        final var channelDescriptor = ChannelDescriptor.builder();
        descriptor(channelDescriptor, handle);
        deviceComponentDescriptor(channelDescriptor);
        return channelDescriptor.build();
    }

    public ChannelState channelState() {
        final var channelState = ChannelState.builder();
        deviceComponentState(channelState);
        return channelState.build();
    }

    public NumericMetricDescriptor numericMetricDescriptor(String handle) {
        final var numericMetricDescriptor = NumericMetricDescriptor.builder();
        descriptor(numericMetricDescriptor, handle);
        metricDescriptor(numericMetricDescriptor, handle);
        numericMetricDescriptor.withAveragingPeriod(Duration.ofMillis(10000))
            .withResolution(BigDecimal.ONE)
            .withTechnicalRange(baseTypes.ranges());
        return numericMetricDescriptor.build();
    }

    public NumericMetricState numericMetricState() {
        final var numericMetricState = NumericMetricState.builder();
        metricState(numericMetricState);
        numericMetricState.withActiveAveragingPeriod(Duration.ofMillis(15000))
            .withPhysiologicalRange(baseTypes.ranges());
        var numericMetricValue = NumericMetricValue.builder();
        metricValue(numericMetricValue);
        numericMetricValue.withValue(BigDecimal.TEN);
        numericMetricState.withMetricValue(numericMetricValue.build());
        return numericMetricState.build();
    }

    public StringMetricDescriptor stringMetricDescriptor(String handle) {
        final var stringMetricDescriptor = StringMetricDescriptor.builder();
        descriptor(stringMetricDescriptor, handle);
        metricDescriptor(stringMetricDescriptor, handle);
        return stringMetricDescriptor.build();
    }

    public StringMetricState stringMetricState() {
        final var stringMetricState = StringMetricState.builder();
        metricState(stringMetricState);
        var stringMetricValue = StringMetricValue.builder();
        metricValue(stringMetricValue);
        stringMetricValue.withValue("string-metric-value");
        stringMetricState.withMetricValue(stringMetricValue.build());
        return stringMetricState.build();
    }

    public EnumStringMetricDescriptor enumStringMetricDescriptor(String handle) {
        final var enumStringMetricDescriptor = EnumStringMetricDescriptor.builder();
        descriptor(enumStringMetricDescriptor, handle);
        metricDescriptor(enumStringMetricDescriptor, handle);
        enumStringMetricDescriptor.withAllowedValue(Arrays.asList(baseTypes.allowedValue("enum-value1"), baseTypes.allowedValue("enum-value2")));
        return enumStringMetricDescriptor.build();
    }

    public EnumStringMetricState enumStringMetricState() {
        final var enumStringMetricState = EnumStringMetricState.builder();
        metricState(enumStringMetricState);
        var stringMetricValue = StringMetricValue.builder();
        metricValue(stringMetricValue);
        stringMetricValue.withValue("enum-value1");
        enumStringMetricState.withMetricValue(stringMetricValue.build());
        return enumStringMetricState.build();
    }

    public RealTimeSampleArrayMetricDescriptor realTimeSampleArrayMetricDescriptor(String handle) {
        final var realTimeSampleArrayMetricDescriptor = RealTimeSampleArrayMetricDescriptor.builder();
        descriptor(realTimeSampleArrayMetricDescriptor, handle);
        metricDescriptor(realTimeSampleArrayMetricDescriptor, handle);
        realTimeSampleArrayMetricDescriptor.withResolution(BigDecimal.ONE)
            .withSamplePeriod(Duration.ofMillis(10))
            .withTechnicalRange(baseTypes.ranges());
        return realTimeSampleArrayMetricDescriptor.build();
    }

    public RealTimeSampleArrayMetricState realTimeSampleArrayMetricState() {
        final var realTimeSampleArrayMetricState = RealTimeSampleArrayMetricState.builder();
        metricState(realTimeSampleArrayMetricState);
        var sampleArrayValue = SampleArrayValue.builder();
        metricValue(sampleArrayValue);
        sampleArrayValue.withSamples(Arrays.asList(BigDecimal.valueOf(1), BigDecimal.valueOf(2), BigDecimal.valueOf(3)))
            .withApplyAnnotation(baseTypes.applyAnnotations());
        realTimeSampleArrayMetricState.withMetricValue(sampleArrayValue.build());
        return realTimeSampleArrayMetricState.build();
    }

    public DistributionSampleArrayMetricDescriptor distributionSampleArrayMetricDescriptor(String handle) {
        final var distributionSampleArrayMetricDescriptor = DistributionSampleArrayMetricDescriptor.builder();
        descriptor(distributionSampleArrayMetricDescriptor, handle);
        metricDescriptor(distributionSampleArrayMetricDescriptor, handle);
        distributionSampleArrayMetricDescriptor.withDistributionRange(baseTypes.range())
            .withDomainUnit(baseTypes.codedValue("domain-unit"))
            .withResolution(BigDecimal.ONE)
            .withTechnicalRange(baseTypes.ranges());
        return distributionSampleArrayMetricDescriptor.build();
    }

    public DistributionSampleArrayMetricState distributionSampleArrayMetricState() {
        final var distributionSampleArrayMetricState = DistributionSampleArrayMetricState.builder();
        metricState(distributionSampleArrayMetricState);
        var sampleArrayValue = SampleArrayValue.builder();
        metricValue(sampleArrayValue);
        sampleArrayValue.withSamples(Arrays.asList(BigDecimal.valueOf(1), BigDecimal.valueOf(2), BigDecimal.valueOf(3)));
        sampleArrayValue.withApplyAnnotation(baseTypes.applyAnnotations());
        distributionSampleArrayMetricState.withMetricValue(sampleArrayValue.build());
        return distributionSampleArrayMetricState.build();
    }

    public ScoDescriptor scoDescriptor(String handle) {
        final var scoDescriptor = ScoDescriptor.builder();
        descriptor(scoDescriptor, handle);
        deviceComponentDescriptor(scoDescriptor);
        return scoDescriptor.build();
    }

    public ScoState scoState() {
        final var scoState = ScoState.builder();
        deviceComponentState(scoState);
        scoState.withInvocationRequested(List.of(Handles.OPERATION_0))
            .withInvocationRequired(List.of(Handles.OPERATION_1))
            .withOperationGroup(List.of(baseTypes.operationGroup("operation-group")));
        return scoState.build();
    }

    public ActivateOperationDescriptor activateOperationDescriptor(String handle, String targetHandle) {
        final var activateOperationDescriptor = ActivateOperationDescriptor.builder();
        descriptor(activateOperationDescriptor, handle);
        operationDescriptor(activateOperationDescriptor, targetHandle);
        activateOperationDescriptor.withArgument(Arrays.asList(baseTypes.argument("argument1"), baseTypes.argument("argument2")));
        return activateOperationDescriptor.build();
    }

    public ActivateOperationState activateOperationState() {
        final var activateOperationState = ActivateOperationState.builder();
        operationState(activateOperationState);
        return activateOperationState.build();
    }

    public SetStringOperationDescriptor setStringOperationDescriptor(String handle, String targetHandle) {
        final var setStringOperationDescriptor = SetStringOperationDescriptor.builder();
        descriptor(setStringOperationDescriptor, handle);
        operationDescriptor(setStringOperationDescriptor, targetHandle);
        setStringOperationDescriptor.withMaxLength(BigInteger.valueOf(255));
        return setStringOperationDescriptor.build();
    }

    public SetStringOperationState setStringOperationState() {
        final var setStringOperationState = SetStringOperationState.builder();
        operationState(setStringOperationState);
        var allowedValues = SetStringOperationState.AllowedValues.builder();
        allowedValues.withValue(Arrays.asList("allowed-value1", "allowed-value2"));
        setStringOperationState.withAllowedValues(allowedValues.build());
        return setStringOperationState.build();
    }

    public SetValueOperationDescriptor setValueOperationDescriptor(String handle, String targetHandle) {
        final var setValueOperationDescriptor = SetValueOperationDescriptor.builder();
        descriptor(setValueOperationDescriptor, handle);
        operationDescriptor(setValueOperationDescriptor, targetHandle);
        return setValueOperationDescriptor.build();
    }

    public SetValueOperationState setValueOperationState() {
        final var setValueOperationState = SetValueOperationState.builder();
        operationState(setValueOperationState);
        setValueOperationState.withAllowedRange(baseTypes.ranges());
        return setValueOperationState.build();
    }

    public SetComponentStateOperationDescriptor setComponentStateOperationDescriptor(String handle, String targetHandle) {
        final var setComponentStateOperationDescriptor = SetComponentStateOperationDescriptor.builder();
        descriptor(setComponentStateOperationDescriptor, handle);
        operationDescriptor(setComponentStateOperationDescriptor, targetHandle);
        return setComponentStateOperationDescriptor.build();
    }

    public SetComponentStateOperationState setComponentStateOperationState() {
        final var setComponentStateOperationState = SetComponentStateOperationState.builder();
        operationState(setComponentStateOperationState);
        return setComponentStateOperationState.build();
    }

    public SetMetricStateOperationDescriptor setMetricStateOperationDescriptor(String handle, String targetHandle) {
        final var setMetricStateOperationDescriptor = SetMetricStateOperationDescriptor.builder();
        descriptor(setMetricStateOperationDescriptor, handle);
        operationDescriptor(setMetricStateOperationDescriptor, targetHandle);
        return setMetricStateOperationDescriptor.build();
    }

    public SetMetricStateOperationState setMetricStateOperationState() {
        final var setMetricStateOperationState = SetMetricStateOperationState.builder();
        operationState(setMetricStateOperationState);
        return setMetricStateOperationState.build();
    }

    public SetAlertStateOperationDescriptor setAlertStateOperationDescriptor(String handle, String targetHandle) {
        final var setAlertStateOperationDescriptor = SetAlertStateOperationDescriptor.builder();
        descriptor(setAlertStateOperationDescriptor, handle);
        operationDescriptor(setAlertStateOperationDescriptor, targetHandle);
        return setAlertStateOperationDescriptor.build();
    }

    public SetAlertStateOperationState setAlertStateOperationState() {
        final var setAlertStateOperationState = SetAlertStateOperationState.builder();
        operationState(setAlertStateOperationState);
        return setAlertStateOperationState.build();
    }

    public SetContextStateOperationDescriptor setContextStateOperationDescriptor(String handle, String targetHandle) {
        final var setContextStateOperationDescriptor = SetContextStateOperationDescriptor.builder();
        descriptor(setContextStateOperationDescriptor, handle);
        operationDescriptor(setContextStateOperationDescriptor, targetHandle);
        return setContextStateOperationDescriptor.build();
    }

    public SetContextStateOperationState setContextStateOperationState() {
        final var setContextStateOperationState = SetContextStateOperationState.builder();
        operationState(setContextStateOperationState);
        return setContextStateOperationState.build();
    }

    private void descriptor(AbstractDescriptor.Builder<?> descriptor, String handle) {
        descriptor(descriptor, handle, RetrievabilityMethod.EP);
    }

    private void descriptor(AbstractDescriptor.Builder<?> descriptor, String handle, RetrievabilityMethod retrievabilityMethod) {
        descriptor
            .withSafetyClassification(SafetyClassification.MED_A)
            .withHandle(handle)
            .withType(baseTypes.codedValue(handle + "-code"))
            .withExtension(
                ExtensionType.builder()
                    .withAny(baseTypes.retrievability(retrievabilityMethod))
                    .build()
            );
    }

    private void deviceComponentDescriptor(AbstractDeviceComponentDescriptor.Builder<?> descriptor) {
        descriptor.withProductionSpecification(baseTypes.productionSpecifications());
    }

    private void deviceComponentState(AbstractDeviceComponentState.Builder<?> state) {
        state.withActivationState(ComponentActivation.ON)
            .withCalibrationInfo(baseTypes.calibrationInfo())
            .withNextCalibration(baseTypes.calibrationInfo())
            .withOperatingCycles(100)
            .withOperatingHours(1000L)
            .withPhysicalConnector(baseTypes.physicalConnectorInfo());
    }

    private void contextState(AbstractContextState.Builder<?> state, String handle) {
        state.withHandle(handle)
            .withBindingStartTime(Instant.ofEpochMilli(1580152377910L))
            .withBindingMdibVersion(BigInteger.ZERO)
            .withContextAssociation(ContextAssociation.ASSOC)
            .withIdentification(Arrays.asList(baseTypes.instanceIdentifier(handle + "id0"),
                baseTypes.instanceIdentifier(handle + "id1")))
            .withValidator(Arrays.asList(baseTypes.instanceIdentifier(handle + "validator0"),
                baseTypes.instanceIdentifier(handle + "validator1")));
    }

    private void alertState(AbstractAlertState.Builder<?> state) {
        state.withActivationState(AlertActivation.ON);
    }

    private void metricDescriptor(AbstractMetricDescriptor.Builder<?> descriptor, String handle) {
        descriptor.withActivationDuration(Duration.ofMillis(2000))
            .withBodySite(baseTypes.codedValues("body-site"))
            .withDerivationMethod(DerivationMethod.AUTO)
            .withDeterminationPeriod(Duration.ofMillis(1000))
            .withMaxDelayTime(Duration.ofMillis(100))
            .withLifeTimePeriod(Duration.ofMillis(2000))
            .withMaxMeasurementTime(Duration.ofMillis(200))
            .withMetricCategory(MetricCategory.MSRMT)
            .withMetricAvailability(MetricAvailability.CONT)
            .withUnit(baseTypes.codedValue("unit"))
            .withRelation(List.of(baseTypes.relation(handle)));
    }

    private void metricState(AbstractMetricState.Builder<?> state) {
        state.withActivationState(ComponentActivation.ON)
            .withActiveDeterminationPeriod(Duration.ofMillis(2000))
            .withBodySite(baseTypes.codedValues("state-body-site"))
            .withLifeTimePeriod(Duration.ofMillis(3000));
    }

    private void metricValue(AbstractMetricValue.Builder<?> value) {
        value.withAnnotation(baseTypes.annotations("metric-value-annotation"))
            .withDeterminationTime(Instant.ofEpochMilli(1580152377910L))
            .withMetricQuality(baseTypes.metricQuality())
            .withStartTime(Instant.ofEpochMilli(1580152377910L).minus(Duration.ofSeconds(10)))
            .withStopTime(Instant.ofEpochMilli(1580152377910L));
    }

    private void operationDescriptor(AbstractOperationDescriptor.Builder<?> descriptor, String targetHandle) {
        descriptor.withAccessLevel(AbstractOperationDescriptor.AccessLevel.OTH)
            .withInvocationEffectiveTimeout(Duration.ofMillis(10000))
            .withMaxTimeToFinish(Duration.ofMillis(500))
            .withOperationTarget(targetHandle)
            .withRetriggerable(false);
    }

    private void operationState(AbstractOperationState.Builder<?> state) {
        state.withOperatingMode(OperatingMode.EN);
    }
}
