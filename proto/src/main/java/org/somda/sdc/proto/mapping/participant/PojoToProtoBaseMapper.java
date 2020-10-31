package org.somda.sdc.proto.mapping.participant;

import com.google.inject.Inject;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.proto.mapping.Util;
import org.somda.sdc.proto.model.biceps.*;

import java.util.List;
import java.util.stream.Collectors;

public class PojoToProtoBaseMapper {


    @Inject
    PojoToProtoBaseMapper() {
    }

    public OperatingJurisdictionMsg mapOperatingJurisdiction(
            OperatingJurisdiction operatingJurisdiction) {
        return OperatingJurisdictionMsg.newBuilder()
                .setInstanceIdentifier(mapInstanceIdentifier(operatingJurisdiction)).build();
    }

    public List<LocalizedTextMsg> mapLocalizedTexts(List<LocalizedText> localizedTexts) {
        return localizedTexts.stream().map(this::mapLocalizedText).collect(Collectors.toList());
    }

//    public List<InstanceIdentifierMsg> mapInstanceIdentifiers(List<InstanceIdentifier> instanceIdentifier) {
//        var builder = InstanceIdentifierMsg.newBuilder();
//        builder.setARoot(Util.toStringValue(instanceIdentifier.getRootName()));
//        builder.setAExtension(Util.toStringValue(instanceIdentifier.getExtensionName()));
//        Util.doIfNotNull(instanceIdentifier.getType(), codedValue -> builder.setType(mapCodedValue(codedValue)));
//        builder.addAllIdentifierName(mapLocalizedTexts(instanceIdentifier.getIdentifierName()));
//        return builder.build();
//    }

    public InstanceIdentifierOneOfMsg mapInstanceIdentifierOneOf(InstanceIdentifier instanceIdentifier) {
        return InstanceIdentifierOneOfMsg.newBuilder()
                .setInstanceIdentifier(mapInstanceIdentifier(instanceIdentifier)).build();
    }

    public InstanceIdentifierMsg mapInstanceIdentifier(InstanceIdentifier instanceIdentifier) {
        var builder = InstanceIdentifierMsg.newBuilder();
        Util.doIfNotNull(instanceIdentifier.getRootName(), it -> builder.setARoot(Util.toStringValue(it)));
        Util.doIfNotNull(instanceIdentifier.getExtensionName(), it ->
                builder.setAExtension(Util.toStringValue(instanceIdentifier.getExtensionName())));
        Util.doIfNotNull(instanceIdentifier.getType(), codedValue -> builder.setType(mapCodedValue(codedValue)));
        builder.addAllIdentifierName(mapLocalizedTexts(instanceIdentifier.getIdentifierName()));
        return builder.build();
    }

    public List<InstanceIdentifierOneOfMsg> mapInstanceIdentifiers(List<InstanceIdentifier> instanceIdentifiers) {
        return instanceIdentifiers.stream()
                .map(it -> InstanceIdentifierOneOfMsg.newBuilder().setInstanceIdentifier(mapInstanceIdentifier(it))
                        .build())
                .collect(Collectors.toList());
    }

    public CodedValueMsg mapCodedValue(CodedValue codedValue) {
        var builder = CodedValueMsg.newBuilder();
        Util.doIfNotNull(codedValue.getCode(), builder::setACode);
        Util.doIfNotNull(codedValue.getCodingSystem(), it ->
                builder.setACodingSystem(Util.toStringValue(codedValue.getCodingSystem())));
        Util.doIfNotNull(codedValue.getCodingSystemVersion(), it ->
                builder.setACodingSystemVersion(Util.toStringValue(codedValue.getCodingSystemVersion())));
        Util.doIfNotNull(codedValue.getSymbolicCodeName(), it ->
                builder.setASymbolicCodeName(Util.toStringValue(codedValue.getSymbolicCodeName())));

        builder.addAllCodingSystemName(mapLocalizedTexts(codedValue.getCodingSystemName()));
        builder.addAllConceptDescription(mapLocalizedTexts(codedValue.getConceptDescription()));
        builder.addAllTranslation(
                codedValue.getTranslation().stream().map(this::mapTranslation).collect(Collectors.toList())
        );

        return builder.build();
    }

    public CodedValueMsg.TranslationMsg mapTranslation(CodedValue.Translation translation) {
        var builder = CodedValueMsg.TranslationMsg.newBuilder();

        builder.setACode(translation.getCode());
        Util.doIfNotNull(translation.getCodingSystem(), it ->
                builder.setACodingSystem(Util.toStringValue(translation.getCodingSystem())));
        Util.doIfNotNull(translation.getCodingSystemVersion(), it ->
                builder.setACodingSystemVersion(Util.toStringValue(translation.getCodingSystemVersion())));

        return builder.build();
    }

    public LocalizedTextMsg mapLocalizedText(LocalizedText localizedText) {
        var builder = LocalizedTextMsg.newBuilder();
        builder.setALang(Util.toStringValue(localizedText.getLang()));
        Util.doIfNotNull(localizedText.getValue(), builder::setString);
        builder.setAVersion(Util.toUInt64(localizedText.getVersion()));
        builder.setARef(Util.toStringValue(localizedText.getRef()));
        Util.doIfNotNull(localizedText.getTextWidth(), width ->
                builder.setATextWidth(Util.mapToProtoEnum(width, LocalizedTextWidthMsg.class)));
        return builder.build();
    }

    AbstractDescriptorMsg mapAbstractDescriptor(AbstractDescriptor abstractDescriptor) {
        var builder = AbstractDescriptorMsg.newBuilder();
        builder.setADescriptorVersion(Util.toUInt64(abstractDescriptor.getDescriptorVersion()));
        Util.doIfNotNull(abstractDescriptor.getHandle(), builder::setAHandle);
        Util.doIfNotNull(abstractDescriptor.getSafetyClassification(), safetyClassification ->
                builder.setASafetyClassification(Util.mapToProtoEnum(safetyClassification, SafetyClassificationMsg.class)));
        Util.doIfNotNull(abstractDescriptor.getType(), codedValue ->
                builder.setType(mapCodedValue(codedValue)));
        return builder.build();
    }

    AbstractStateMsg mapAbstractState(AbstractState abstractState) {
        var builder = AbstractStateMsg.newBuilder();
        builder.setADescriptorHandle(abstractState.getDescriptorHandle());
        builder.setADescriptorVersion(Util.toUInt64(abstractState.getDescriptorVersion()));
        builder.setAStateVersion(Util.toUInt64(abstractState.getStateVersion()));
        return builder.build();
    }

    AbstractMultiStateMsg mapAbstractMultiState(AbstractMultiState abstractMultiState) {
        var builder = AbstractMultiStateMsg.newBuilder();
        Util.doIfNotNull(abstractMultiState.getCategory(), codedValue ->
                builder.setCategory(mapCodedValue(codedValue)));
        builder.setAHandle(abstractMultiState.getHandle());
        builder.setAbstractState(mapAbstractState(abstractMultiState));
        return builder.build();
    }

    BaseDemographicsMsg mapBaseDemographics(BaseDemographics baseDemographics) {
        var builder = BaseDemographicsMsg.newBuilder();
        Util.doIfNotNull(baseDemographics.getBirthname(), name -> builder.setBirthname(Util.toStringValue(name)));
        Util.doIfNotNull(baseDemographics.getGivenname(), name -> builder.setGivenname(Util.toStringValue(name)));
        baseDemographics.getMiddlename().forEach(builder::addMiddlename);
        Util.doIfNotNull(baseDemographics.getFamilyname(), name -> builder.setFamilyname(Util.toStringValue(name)));
        Util.doIfNotNull(baseDemographics.getTitle(), title -> builder.setTitle(Util.toStringValue(title)));
        return builder.build();
    }

    MeasurementMsg mapMeasurement(Measurement measurement) {
        var builder = MeasurementMsg.newBuilder();
        builder.setAMeasuredValue(measurement.getMeasuredValue().toPlainString());
        builder.setMeasurementUnit(mapCodedValue(measurement.getMeasurementUnit()));
        return builder.build();
    }

    RangeMsg mapRange(Range range) {
        var builder = RangeMsg.newBuilder();

        Util.doIfNotNull(range.getAbsoluteAccuracy(), accuracy ->
                builder.setAAbsoluteAccuracy(Util.toStringValue(accuracy.toPlainString()))
        );
        Util.doIfNotNull(range.getRelativeAccuracy(), accuracy ->
                builder.setARelativeAccuracy(Util.toStringValue(accuracy.toPlainString()))
        );
        Util.doIfNotNull(range.getAbsoluteAccuracy(), accuracy ->
                builder.setAAbsoluteAccuracy(Util.toStringValue(accuracy.toPlainString()))
        );
        Util.doIfNotNull(range.getLower(), accuracy ->
                builder.setALower(Util.toStringValue(accuracy.toPlainString()))
        );
        Util.doIfNotNull(range.getUpper(), accuracy ->
                builder.setAUpper(Util.toStringValue(accuracy.toPlainString()))
        );
        Util.doIfNotNull(range.getStepWidth(), accuracy ->
                builder.setAStepWidth(Util.toStringValue(accuracy.toPlainString()))
        );
        return builder.build();
    }


    public PhysicalConnectorInfoMsg mapPhysicalConnectorInfo(PhysicalConnectorInfo physicalConnectorInfo) {
        var builder = PhysicalConnectorInfoMsg.newBuilder();

        builder.setANumber(Util.toInt32(physicalConnectorInfo.getNumber()));
        builder.addAllLabel(mapLocalizedTexts(physicalConnectorInfo.getLabel()));

        return builder.build();
    }
}
