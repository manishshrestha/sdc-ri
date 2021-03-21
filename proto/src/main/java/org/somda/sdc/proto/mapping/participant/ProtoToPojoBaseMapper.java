package org.somda.sdc.proto.mapping.participant;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.proto.mapping.Util;
import org.somda.protosdc.proto.model.biceps.*;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class ProtoToPojoBaseMapper {
    private static final Logger LOG = LogManager.getLogger(ProtoToPojoOneOfMapper.class);
    private final Logger instanceLogger;

    @Inject
    ProtoToPojoBaseMapper(@Named(org.somda.sdc.common.CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
    }

    void map(AbstractDescriptor pojo, AbstractDescriptorMsg protoMsg) {
        pojo.setHandle(protoMsg.getAHandle().getString());
        pojo.setDescriptorVersion(Util.optionalVersionCounter(protoMsg, "ADescriptorVersion"));
        if (protoMsg.hasType()) {
            pojo.setType(map(protoMsg.getType()));
        }
        pojo.setSafetyClassification(Util.mapToPojoEnum(protoMsg, "ASafetyClassification", SafetyClassification.class));

        // TODO: Extension
//        pojo.setExtension();

    }

    void map(AbstractState pojo, AbstractStateMsg protoMsg) {
        pojo.setDescriptorHandle(protoMsg.getADescriptorHandle().getString());
        pojo.setDescriptorVersion(Util.optionalReferencedVersion(protoMsg, "ADescriptorVersion"));
        pojo.setStateVersion(Util.optionalVersionCounter(protoMsg, "AStateVersion"));
    }

    void map(AbstractMultiState pojo, AbstractMultiStateMsg protoMsg) {
        pojo.setHandle(protoMsg.getAHandle().getString());
        Util.doIfNotNull(Util.optional(protoMsg, "Category", CodedValueMsg.class), category ->
                pojo.setCategory(map(category)));
        map(pojo, protoMsg.getAbstractState());
    }

    public InstanceIdentifier map(@Nullable InstanceIdentifierMsg protoMsg) {
        if (protoMsg == null) {
            return null;
        }

        var pojo = new InstanceIdentifier();
        Util.doIfNotNull(
                Util.optional(protoMsg, "AExtension", InstanceIdentifierMsg.AExtensionMsg.class),
                ext -> pojo.setExtensionName(ext.getString())
        );
        Util.doIfNotNull(Util.optional(
                protoMsg, "ARoot", InstanceIdentifierMsg.ARootMsg.class),
                msg -> pojo.setRootName(msg.getAnyURI())
        );

        pojo.setType(map(Util.optional(protoMsg, "Type", CodedValueMsg.class)));
        pojo.setIdentifierName(mapLocalizedTexts(protoMsg.getIdentifierNameList()));
        return pojo;
    }

    public List<InstanceIdentifier> mapInstanceIdentifiers(List<InstanceIdentifierOneOfMsg> protoMsgs) {
        return protoMsgs.stream().map(it -> {
            switch (it.getInstanceIdentifierOneOfCase()) {
                case INSTANCE_IDENTIFIER:
                    return map(it.getInstanceIdentifier());
                case OPERATING_JURISDICTION:
                case INSTANCEIDENTIFIERONEOF_NOT_SET:
                default:
                    instanceLogger.error("Instance identifier type not supported in context identification: {}",
                            it.getInstanceIdentifierOneOfCase());
                    return new InstanceIdentifier();
            }
        }).collect(Collectors.toList());
    }

    public OperatingJurisdiction map(@Nullable OperatingJurisdictionMsg protoMsg) {
        if (protoMsg == null) {
            return null;
        }
        var mappedInstanceIdentifier = map(protoMsg.getInstanceIdentifier());
        var pojo = new OperatingJurisdiction();
        pojo.setExtensionName(mappedInstanceIdentifier.getExtensionName());
        pojo.setRootName(mappedInstanceIdentifier.getRootName());
        pojo.setIdentifierName(mappedInstanceIdentifier.getIdentifierName());
        pojo.setType(mappedInstanceIdentifier.getType());
        return pojo;
    }

    public CodedValue map(@Nullable CodedValueMsg protoMsg) {
        if (protoMsg == null) {
            return null;
        }

        var pojo = new CodedValue();
        pojo.setCode(protoMsg.getACode().getString());
        pojo.setCodingSystem(Util.optionalStr(protoMsg, "ACodingSystem"));
        pojo.setCodingSystemVersion(Util.optionalStr(protoMsg, "ACodingSystemVersion"));
        Util.doIfNotNull(
                Util.optional(protoMsg, "ASymbolicCodeName", SymbolicCodeNameMsg.class),
                name -> pojo.setSymbolicCodeName(name.getString())
        );

        pojo.setCodingSystemName(mapLocalizedTexts(protoMsg.getCodingSystemNameList()));
        pojo.setConceptDescription(mapLocalizedTexts(protoMsg.getConceptDescriptionList()));
        protoMsg.getTranslationList().forEach(translationMsg ->
                pojo.getTranslation().add(map(translationMsg))
        );

        // TODO: Extension
//        pojo.setExtension();
        return pojo;
    }

    public CodedValue.Translation map(@Nullable CodedValueMsg.TranslationMsg protoMsg) {
        if (protoMsg == null) {
            return null;
        }

        var pojo = new CodedValue.Translation();
        pojo.setCode(protoMsg.getACode().getString());
        Util.doIfNotNull(Util.optionalStr(protoMsg, "ACodingSystem"),
                system -> pojo.setCodingSystem(system));

        Util.doIfNotNull(Util.optionalStr(protoMsg, "ACodingSystemVersion"),
                version -> pojo.setCodingSystemVersion(version));

        // TODO: Extension
//        pojo.setExtension();
        return pojo;
    }

    public LocalizedText map(@Nullable LocalizedTextMsg protoMsg) {
        if (protoMsg == null) {
            return null;
        }

        var pojo = new LocalizedText();
        pojo.setLang(Util.optionalStr(protoMsg, "ALang"));
        Util.doIfNotNull(
                Util.optional(protoMsg, "ARef", LocalizedTextRefMsg.class),
                ref -> pojo.setRef(ref.getString())
        );
        pojo.setVersion(Util.optionalReferencedVersion(protoMsg, "AVersion"));
        if (protoMsg.hasATextWidth()) {
            pojo.setTextWidth(Util.mapToPojoEnum(protoMsg, "ATextWidth", LocalizedTextWidth.class));
        }
        pojo.setValue(protoMsg.getLocalizedTextContent().getString());
        return pojo;
    }

    public List<LocalizedText> mapLocalizedTexts(List<LocalizedTextMsg> protoMsgs) {
        return protoMsgs.stream().map(this::map).collect(Collectors.toList());
    }

    public LocationDetail map(LocationDetailMsg protoMsg) {
        var pojo = new LocationDetail();
        pojo.setBed(Util.optionalStr(protoMsg, "ABed"));
        pojo.setBuilding(Util.optionalStr(protoMsg, "ABuilding"));
        pojo.setFacility(Util.optionalStr(protoMsg, "AFacility"));
        pojo.setFloor(Util.optionalStr(protoMsg, "AFloor"));
        pojo.setPoC(Util.optionalStr(protoMsg, "APoC"));
        pojo.setRoom(Util.optionalStr(protoMsg, "ARoom"));
        return pojo;
    }

    protected void map(BaseDemographics pojo, BaseDemographicsMsg protoMsg) {
        Util.doIfNotNull(Util.optionalStr(protoMsg, "Givenname"), pojo::setGivenname);
        protoMsg.getMiddlenameList().forEach(name -> pojo.getMiddlename().add(name));
        Util.doIfNotNull(Util.optionalStr(protoMsg, "Familyname"), pojo::setFamilyname);
        Util.doIfNotNull(Util.optionalStr(protoMsg, "Birthname"), pojo::setBirthname);
        Util.doIfNotNull(Util.optionalStr(protoMsg, "Title"), pojo::setTitle);
    }

    public Measurement map(MeasurementMsg protoMsg) {
        var pojo = new Measurement();
        pojo.setMeasuredValue(new BigDecimal(protoMsg.getAMeasuredValue()));
        pojo.setMeasurementUnit(map(protoMsg.getMeasurementUnit()));
        return pojo;
    }

    public BaseDemographics map(BaseDemographicsMsg protoMsg) {
        var pojo = new BaseDemographics();
        pojo.setGivenname(Util.optionalStr(protoMsg, "Givenname"));
        protoMsg.getMiddlenameList().forEach(name -> pojo.getMiddlename().add(name));
        pojo.setFamilyname(Util.optionalStr(protoMsg, "Familyname"));
        pojo.setBirthname(Util.optionalStr(protoMsg, "Birthname"));
        pojo.setTitle(Util.optionalStr(protoMsg, "Title"));
        return pojo;
    }

    public Range map(RangeMsg protoMsg) {
        var pojoValue = new Range();
        pojoValue.setStepWidth(Util.optionalBigDecimalOfString(protoMsg, "AStepWidth"));
        pojoValue.setRelativeAccuracy(Util.optionalBigDecimalOfString(protoMsg, "ARelativeAccuracy"));
        pojoValue.setLower(Util.optionalBigDecimalOfString(protoMsg, "ALower"));
        pojoValue.setUpper(Util.optionalBigDecimalOfString(protoMsg, "AUpper"));
        pojoValue.setAbsoluteAccuracy(Util.optionalBigDecimalOfString(protoMsg, "AAbsoluteAccuracy"));
        return pojoValue;
    }


    public PhysicalConnectorInfo map(PhysicalConnectorInfoMsg protoMsg) {
        var pojo = new PhysicalConnectorInfo();
        pojo.setNumber(Util.optionalIntOfInt(protoMsg, "ANumber"));
        pojo.setLabel(mapLocalizedTexts(protoMsg.getLabelList()));
        return pojo;
    }

    public String map(HandleRefMsg handleRefMsg) {
        return handleRefMsg.getString();
    }

    public int map(TransactionIdMsg transactionIdMsg) {
        return transactionIdMsg.getUnsignedInt();
    }
}
