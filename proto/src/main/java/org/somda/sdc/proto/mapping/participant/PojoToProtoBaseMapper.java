package org.somda.sdc.proto.mapping.participant;

import com.google.inject.Inject;
import org.somda.sdc.biceps.model.participant.AbstractDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractDeviceComponentDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractMultiState;
import org.somda.sdc.biceps.model.participant.AbstractState;
import org.somda.sdc.biceps.model.participant.CodedValue;
import org.somda.sdc.biceps.model.participant.InstanceIdentifier;
import org.somda.sdc.biceps.model.participant.LocalizedText;
import org.somda.sdc.biceps.model.participant.OperatingJurisdiction;
import org.somda.sdc.proto.mapping.Util;
import org.somda.sdc.proto.model.biceps.AbstractDescriptorMsg;
import org.somda.sdc.proto.model.biceps.AbstractDeviceComponentDescriptorMsg;
import org.somda.sdc.proto.model.biceps.AbstractMultiStateMsg;
import org.somda.sdc.proto.model.biceps.AbstractStateMsg;
import org.somda.sdc.proto.model.biceps.CodedValueMsg;
import org.somda.sdc.proto.model.biceps.InstanceIdentifierMsg;
import org.somda.sdc.proto.model.biceps.InstanceIdentifierOneOfMsg;
import org.somda.sdc.proto.model.biceps.LocalizedTextMsg;
import org.somda.sdc.proto.model.biceps.LocalizedTextWidthMsg;
import org.somda.sdc.proto.model.biceps.OperatingJurisdictionMsg;
import org.somda.sdc.proto.model.biceps.SafetyClassificationMsg;

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
        builder.addAllConceptDescription(mapLocalizedTexts(codedValue.getConceptDescription()));
        builder.addAllCodingSystemName(mapLocalizedTexts(codedValue.getCodingSystemName()));
        return builder.build();
    }

    public LocalizedTextMsg mapLocalizedText(LocalizedText localizedText) {
        var builder = LocalizedTextMsg.newBuilder();
        builder.setALang(Util.toStringValue(localizedText.getLang()));
        Util.doIfNotNull(localizedText.getValue(), builder::setString);
        builder.setAVersion(Util.toUInt64(localizedText.getVersion()));
        builder.setARef(Util.toStringValue(localizedText.getRef()));
        Util.doIfNotNull(localizedText.getTextWidth(), width ->
                builder.setATextWidth(Util.mapToPojoEnum(width, "LocalizedTextWidth", LocalizedTextWidthMsg.class)));
        return builder.build();
    }

    public AbstractDeviceComponentDescriptorMsg.ProductionSpecificationMsg mapProductionSpecification(
            AbstractDeviceComponentDescriptor.ProductionSpecification productionSpecification) {
        var builder = AbstractDeviceComponentDescriptorMsg.ProductionSpecificationMsg
                .newBuilder();
        var compIdBuilder = InstanceIdentifierOneOfMsg.newBuilder();
        Util.doIfNotNull(productionSpecification.getComponentId(), instanceIdentifier ->
                compIdBuilder.setInstanceIdentifier(mapInstanceIdentifier(instanceIdentifier)));
        builder.setComponentId(compIdBuilder);
        Util.doIfNotNull(productionSpecification.getProductionSpec(), builder::setProductionSpec);
        Util.doIfNotNull(productionSpecification.getSpecType(), codedValue ->
                builder.setSpecType(mapCodedValue(codedValue)));
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
}
