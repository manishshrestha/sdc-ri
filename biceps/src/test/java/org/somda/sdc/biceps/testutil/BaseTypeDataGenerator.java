package org.somda.sdc.biceps.testutil;

import org.somda.sdc.biceps.model.message.Retrievability;
import org.somda.sdc.biceps.model.message.RetrievabilityInfo;
import org.somda.sdc.biceps.model.message.RetrievabilityMethod;
import org.somda.sdc.biceps.model.participant.*;

import javax.xml.namespace.QName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BaseTypeDataGenerator {
    private final ObjectFactory participantFactory;
    private final org.somda.sdc.biceps.model.message.ObjectFactory messageFactory;

    public BaseTypeDataGenerator() {
        this.participantFactory = new ObjectFactory();
        this.messageFactory = new org.somda.sdc.biceps.model.message.ObjectFactory();
    }

    public ApprovedJurisdictions approvedJurisdictions() {
        return ApprovedJurisdictions.builder()
            .addApprovedJurisdiction(
                Arrays.asList(instanceIdentifier("approved-jurisdiction1"),
                    instanceIdentifier("approved-jurisdiction2"))
            )
            .build();
    }

    public InstanceIdentifier instanceIdentifier(String extension) {
        final String root = "http://test-root";
        var instanceIdentifier = InstanceIdentifier.builder()
            .withRootName(root)
            .withExtensionName(extension);
        return instanceIdentifier.build();
    }

    public OperatingJurisdiction operatingJurisdiction(String extension) {
        final String root = "http://test-root";
        var instanceIdentifier = OperatingJurisdiction.builder()
            .withRootName(root)
            .withExtensionName(extension);
        return instanceIdentifier.build();
    }

    public LocalDateTime localDateTime() {
        final String isoDateTime = "2009-05-07T13:05:45.678-04:00";
        return LocalDateTime.parse(isoDateTime, DateTimeFormatter.ISO_DATE_TIME);
    }

    public List<LocalizedText> localizedTexts() {
        final var localizedTextEn = LocalizedText.builder()
            .withLang("en")
            .withTextWidth(LocalizedTextWidth.M)
            .withValue("This is a sample LocalizedText with text width M")
            .build();

        final LocalizedText localizedTextDe = LocalizedText.builder()
            .withLang("de")
            .withTextWidth(LocalizedTextWidth.L)
            .withValue("Dies ist ein Beispiel-LocalizedText mit Textbreite L")
            .build();

        return Arrays.asList(localizedTextEn, localizedTextDe);
    }

    public List<AbstractDeviceComponentDescriptor.ProductionSpecification> productionSpecifications() {
        final var ps1 = AbstractDeviceComponentDescriptor.ProductionSpecification.builder()
            .withComponentId(instanceIdentifier("component-id1"))
            .withProductionSpec("production-specification1")
            .withSpecType(codedValue("production-specification1"))
            .build();

        final var ps2 = AbstractDeviceComponentDescriptor.ProductionSpecification.builder()
            .withComponentId(instanceIdentifier("component-id2"))
            .withProductionSpec("production-specification2")
            .withSpecType(codedValue("production-specification2"))
            .build();

        return Arrays.asList(ps1, ps2);
    }

    public CodedValue codedValue(String codeId) {
        final var translation1 = CodedValue.Translation.builder()
            .withCode(codeId + "-translation1")
            .withCodingSystem("http://test-coding-system-translation1")
            .withCodingSystemVersion("2018");
        final var translation2 = CodedValue.Translation.builder()
            .withCode(codeId + "-translation2")
            .withCodingSystem("http://test-coding-system-translation2")
            .withCodingSystemVersion("2017");

        final var codedValue = CodedValue.builder()
            .withCode(codeId)
            .withCodingSystem("http://test-coding-system")
            .withCodingSystemVersion("2019")
            .withCodingSystemName(localizedTexts())
            .withSymbolicCodeName("SYMBOLIC_NAME_" + codeId.toUpperCase())
            .withConceptDescription(localizedTexts())
            .withTranslation(Arrays.asList(translation1.build(), translation2.build()));

        return codedValue.build();
    }

    public List<CodedValue> codedValues(String codeId) {
        return Arrays.asList(codedValue(codeId + "1"), codedValue(codeId + "2"));
    }

    public CalibrationInfo calibrationInfo() {
        final var calibrationInfo = CalibrationInfo.builder();

        final var calibrationDocumentation = CalibrationInfo.CalibrationDocumentation.builder()
            .withDocumentation(localizedTexts());

        final var calibrationResult1 = CalibrationInfo.CalibrationDocumentation.CalibrationResult.builder()
            .withCode(codedValue("calibration-result1"))
            .withValue(measurement(BigDecimal.ONE));
        final var calibrationResult2 = CalibrationInfo.CalibrationDocumentation.CalibrationResult.builder()
            .withCode(codedValue("calibration-result2"))
            .withValue(measurement(BigDecimal.TEN));

        calibrationDocumentation.withCalibrationResult(Arrays.asList(calibrationResult1.build(), calibrationResult2.build()));
        calibrationInfo.withCalibrationDocumentation(List.of(calibrationDocumentation.build()));
        return calibrationInfo.build();
    }

    public Measurement measurement(BigDecimal value) {
        final var measurement = Measurement.builder()
            .withMeasuredValue(value)
            .withMeasurementUnit(codedValue("measurement"));
        return measurement.build();
    }

    public PhysicalConnectorInfo physicalConnectorInfo() {
        final var physicalConnectorInfo = PhysicalConnectorInfo.builder()
            .withLabel(localizedTexts())
            .withNumber(7);
        return physicalConnectorInfo.build();
    }

    public PatientDemographicsCoreData patientDemographicsCoreData() {
        final var patientDemographicsCoreData = PatientDemographicsCoreData.builder()
            .withDateOfBirth("1984-12-23")
            .withHeight(measurement(BigDecimal.valueOf(180)))
            .withPatientType(PatientType.AD)
            .withRace(codedValue("race"))
            .withBirthname("Birthname")
            .withFamilyname("Familyname")
            .withGivenname("Givenname")
            .withMiddlename(List.of("Middlename"))
            .withSex(Sex.M)
            .withWeight(measurement(BigDecimal.valueOf(80)))
            .withTitle("PhD");
        return patientDemographicsCoreData.build();
    }

    public LocationDetail locationDetail() {
        final var locationDetail = LocationDetail.builder()
            .withBed("bed1")
            .withBuilding("building1")
            .withFacility("facility1")
            .withFloor("floor1")
            .withPoC("poc1")
            .withRoom("room1");
        return locationDetail.build();
    }

    public SystemSignalActivation systemSignalActivation(AlertSignalManifestation manifestation) {
        var systemSignalActivation = SystemSignalActivation.builder()
            .withManifestation(manifestation)
            .withState(AlertActivation.ON);
        return systemSignalActivation.build();
    }

    public CauseInfo causeInfo() {
        final var causeInfo = CauseInfo.builder()
            .withDescription(localizedTexts())
            .withRemedyInfo(remedyInfo());
        return causeInfo.build();
    }

    public RemedyInfo remedyInfo() {
        final var remedyInfo = RemedyInfo.builder()
            .withDescription(localizedTexts());
        return remedyInfo.build();
    }

    public Range range() {
        return range(BigDecimal.ZERO, BigDecimal.TEN);
    }

    public Range range(BigDecimal lower, BigDecimal upper) {
        final var range = Range.builder()
            .withAbsoluteAccuracy(BigDecimal.ONE)
            .withRelativeAccuracy(BigDecimal.ONE)
            .withLower(lower)
            .withUpper(upper)
            .withStepWidth(BigDecimal.ONE);
        return range.build();
    }

    public AbstractMetricDescriptor.Relation relation(String handle) {
        var relation = AbstractMetricDescriptor.Relation.builder()
            .withCode(codedValue(handle))
            .withIdentification(instanceIdentifier(handle))
            .withKind(AbstractMetricDescriptor.Relation.Kind.OTH)
            .withEntries(List.of(handle));
        return relation.build();
    }

    public List<Range> ranges() {
        return Arrays.asList(range(BigDecimal.ZERO, BigDecimal.TEN), range(BigDecimal.valueOf(20), BigDecimal.valueOf(100)));
    }

    public AbstractMetricValue.Annotation annotation(String codeId) {
        final var annotation = AbstractMetricValue.Annotation.builder()
            .withType(codedValue(codeId));
        return annotation.build();
    }

    public List<AbstractMetricValue.Annotation> annotations(String codeId) {
        return Arrays.asList(annotation(codeId + "1"), annotation(codeId + "2"));
    }

    public AbstractMetricValue.MetricQuality metricQuality() {
        final var metricQuality = AbstractMetricValue.MetricQuality.builder()
            .withMode(GenerationMode.DEMO)
            .withQi(BigDecimal.ONE)
            .withValidity(MeasurementValidity.VLD);
        return metricQuality.build();
    }

    public EnumStringMetricDescriptor.AllowedValue allowedValue(String value) {
        final var allowedValue = EnumStringMetricDescriptor.AllowedValue.builder()
            .withCharacteristic(measurement(BigDecimal.ONE))
            .withIdentification(instanceIdentifier(value + "-identifier"))
            .withType(codedValue(value + "-code"))
            .withValue(value);
        return allowedValue.build();
    }

    public List<SampleArrayValue.ApplyAnnotation> applyAnnotations() {
        final var applyAnnotation = SampleArrayValue.ApplyAnnotation.builder()
            .withAnnotationIndex(1)
            .withSampleIndex(1);
        return List.of(applyAnnotation.build());
    }

    public ScoState.OperationGroup operationGroup(String code) {
        var operationGroup = ScoState.OperationGroup.builder()
            .withOperatingMode(OperatingMode.EN)
            .withOperations(Arrays.asList(Handles.OPERATION_0, Handles.OPERATION_1))
            .withType(codedValue(code));
        return operationGroup.build();
    }

    public ActivateOperationDescriptor.Argument argument(String code) {
        var argument = ActivateOperationDescriptor.Argument.builder()
            .withArg(new QName("http://argument-uri", "a-type", "a"))
            .withArgName(codedValue(code));
        return argument.build();
    }

    public Retrievability retrievability(RetrievabilityMethod method) {
        var retrievability = Retrievability.builder();

        var getMethod = RetrievabilityInfo.builder();
        getMethod.withMethod(RetrievabilityMethod.GET);
        var additionalMethod = RetrievabilityInfo.builder();
        additionalMethod.withMethod(method);

        retrievability.addBy(Arrays.asList(getMethod.build(), additionalMethod.build()));
        return retrievability.build() ;
    }
}
