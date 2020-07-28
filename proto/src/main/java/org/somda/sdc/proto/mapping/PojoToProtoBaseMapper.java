package org.somda.sdc.proto.mapping;

import com.google.inject.Inject;
import org.somda.sdc.biceps.model.participant.AbstractDeviceComponentDescriptor;
import org.somda.sdc.biceps.model.participant.CodedValue;
import org.somda.sdc.biceps.model.participant.InstanceIdentifier;
import org.somda.sdc.biceps.model.participant.LocalizedText;
import org.somda.sdc.biceps.model.participant.OperatingJurisdiction;
import org.somda.sdc.proto.model.biceps.AbstractDeviceComponentDescriptorMsg;
import org.somda.sdc.proto.model.biceps.CodedValueMsg;
import org.somda.sdc.proto.model.biceps.InstanceIdentifierMsg;
import org.somda.sdc.proto.model.biceps.InstanceIdentifierOneOfMsg;
import org.somda.sdc.proto.model.biceps.LocalizedTextMsg;
import org.somda.sdc.proto.model.biceps.LocalizedTextWidthMsg;
import org.somda.sdc.proto.model.biceps.OperatingJurisdictionMsg;

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
        builder.setARoot(Util.toStringValue(instanceIdentifier.getRootName()));
        builder.setAExtension(Util.toStringValue(instanceIdentifier.getExtensionName()));
        Util.doIfNotNull(instanceIdentifier.getType(), codedValue -> builder.setType(mapCodedValue(codedValue)));
        builder.addAllIdentifierName(mapLocalizedTexts(instanceIdentifier.getIdentifierName()));
        return builder.build();
    }

    public CodedValueMsg mapCodedValue(CodedValue codedValue) {
        var builder = CodedValueMsg.newBuilder();
        Util.doIfNotNull(codedValue.getCode(), builder::setACode);
        builder.setACodingSystem(Util.toStringValue(codedValue.getCodingSystem()));
        builder.setACodingSystemVersion(Util.toStringValue(codedValue.getCodingSystemVersion()));
        builder.setASymbolicCodeName(Util.toStringValue(codedValue.getSymbolicCodeName()));
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
}
