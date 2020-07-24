package org.somda.sdc.proto.mapping;

import com.google.inject.Inject;
import org.somda.sdc.biceps.model.participant.CodedValue;
import org.somda.sdc.biceps.model.participant.ComponentActivation;
import org.somda.sdc.biceps.model.participant.InstanceIdentifier;
import org.somda.sdc.biceps.model.participant.LocalizedText;
import org.somda.sdc.biceps.model.participant.LocalizedTextWidth;
import org.somda.sdc.biceps.model.participant.MdsOperatingMode;
import org.somda.sdc.biceps.model.participant.OperatingJurisdiction;
import org.somda.sdc.proto.model.biceps.CodedValueType;
import org.somda.sdc.proto.model.biceps.ComponentActivationType;
import org.somda.sdc.proto.model.biceps.InstanceIdentifierType;
import org.somda.sdc.proto.model.biceps.LocalizedTextType;
import org.somda.sdc.proto.model.biceps.LocalizedTextWidthType;
import org.somda.sdc.proto.model.biceps.MdsOperatingModeType;
import org.somda.sdc.proto.model.biceps.OperatingJurisdictionType;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class JaxbToProtoBaseMapper {

    @Inject
    JaxbToProtoBaseMapper() {

    }

    public OperatingJurisdictionType.OperatingJurisdiction mapOperatingJurisdiction(
            OperatingJurisdiction operatingJurisdiction) {
        return OperatingJurisdictionType.OperatingJurisdiction.newBuilder()
                .setInstanceIdentifier(mapInstanceIdentifier(operatingJurisdiction)).build();
    }

    public List<LocalizedTextType.LocalizedText> mapLocalizedTexts(List<LocalizedText> localizedTexts) {
        return localizedTexts.stream().map(this::mapLocalizedText).collect(Collectors.toList());
    }

    public InstanceIdentifierType.InstanceIdentifier mapInstanceIdentifier(InstanceIdentifier instanceIdentifier) {
        var builder = InstanceIdentifierType.InstanceIdentifier.newBuilder();
        setOptional(instanceIdentifier.getRootName(), builder::setRoot);
        setOptional(instanceIdentifier.getExtensionName(), builder::setExtension);
        setOptional(instanceIdentifier.getType(), codedValue -> builder.setType(mapCodedValue(codedValue)));
        builder.addAllIdentifierName(mapLocalizedTexts(instanceIdentifier.getIdentifierName()));
        return builder.build();
    }

    public CodedValueType.CodedValue mapCodedValue(CodedValue codedValue) {
        var builder = CodedValueType.CodedValue.newBuilder();
        setOptional(codedValue.getCode(), builder::setACode);
        setOptional(codedValue.getCodingSystem(), builder::setACodingSystem);
        setOptional(codedValue.getCodingSystemVersion(), builder::setACodingSystemVersion);
        setOptional(codedValue.getSymbolicCodeName(), builder::setASymbolicCodeName);
        builder.addAllConceptDescription(mapLocalizedTexts(codedValue.getConceptDescription()));
        builder.addAllCodingSystemName(mapLocalizedTexts(codedValue.getCodingSystemName()));
        return builder.build();
    }

    public LocalizedTextType.LocalizedText mapLocalizedText(LocalizedText localizedText) {
        var builder = LocalizedTextType.LocalizedText.newBuilder();
        setOptional(localizedText.getLang(), builder::setALang);
        setOptional(localizedText.getValue(), builder::setString);
        setOptional(localizedText.getVersion().longValue(), builder::setAVersion);
        setOptional(localizedText.getRef(), builder::setARef);
        setOptional(localizedText.getTextWidth(), localizedTextWidth ->
                builder.setATextWidth(mapLocalizedTextWidth(localizedTextWidth)));
        return builder.build();
    }

    public LocalizedTextWidthType.LocalizedTextWidth mapLocalizedTextWidth(LocalizedTextWidth localizedTextWidth) {
        var builder = LocalizedTextWidthType.LocalizedTextWidth.newBuilder();
        switch (localizedTextWidth) {
            case XS:
                return builder.setType(LocalizedTextWidthType.LocalizedTextWidth.LocalizedTextWidthEnum.XS).build();
            case S:
                return builder.setType(LocalizedTextWidthType.LocalizedTextWidth.LocalizedTextWidthEnum.S).build();
            case M:
                return builder.setType(LocalizedTextWidthType.LocalizedTextWidth.LocalizedTextWidthEnum.M).build();
            case L:
                return builder.setType(LocalizedTextWidthType.LocalizedTextWidth.LocalizedTextWidthEnum.L).build();
            case XL:
                return builder.setType(LocalizedTextWidthType.LocalizedTextWidth.LocalizedTextWidthEnum.XL).build();
            case XXL:
                return builder.setType(LocalizedTextWidthType.LocalizedTextWidth.LocalizedTextWidthEnum.XXL).build();
            default:
                throw new IllegalArgumentException(String.format("Enum %s is not mappable", localizedTextWidth));
        }
    }

    public MdsOperatingModeType.MdsOperatingMode mapMdsOperatingMode(MdsOperatingMode mdsOperatingMode) {
        var builder = MdsOperatingModeType.MdsOperatingMode.newBuilder();
        switch (mdsOperatingMode) {
            case NML:
                return builder.setType(MdsOperatingModeType.MdsOperatingMode.MdsOperatingModeEnum.NML).build();
            case DMO:
                return builder.setType(MdsOperatingModeType.MdsOperatingMode.MdsOperatingModeEnum.DMO).build();
            case SRV:
                return builder.setType(MdsOperatingModeType.MdsOperatingMode.MdsOperatingModeEnum.SRV).build();
            case MTN:
                return builder.setType(MdsOperatingModeType.MdsOperatingMode.MdsOperatingModeEnum.MTN).build();
            default:
                throw new IllegalArgumentException(String.format("Enum %s is not mappable", mdsOperatingMode));
        }
    }

    public ComponentActivationType.ComponentActivation mapComponentActivation(ComponentActivation componentActivation) {
        var builder = ComponentActivationType.ComponentActivation.newBuilder();
        switch (componentActivation) {
            case ON:
                return builder.setType(ComponentActivationType.ComponentActivation.ComponentActivationEnum.ON).build();
            case NOT_RDY:
                return builder.setType(ComponentActivationType.ComponentActivation.ComponentActivationEnum.NOTRDY).build();
            case STND_BY:
                return builder.setType(ComponentActivationType.ComponentActivation.ComponentActivationEnum.STNDBY).build();
            case OFF:
                return builder.setType(ComponentActivationType.ComponentActivation.ComponentActivationEnum.OFF).build();
            case SHTDN:
                return builder.setType(ComponentActivationType.ComponentActivation.ComponentActivationEnum.SHTDN).build();
            case FAIL:
                return builder.setType(ComponentActivationType.ComponentActivation.ComponentActivationEnum.FAIL).build();
            default:
                throw new IllegalArgumentException(String.format("Enum %s is not mappable", componentActivation));
        }
    }

    private <T> void setOptional(@Nullable T value, Consumer<T> consumer) {
        if (value != null) {
            consumer.accept(value);
        }
    }
}
