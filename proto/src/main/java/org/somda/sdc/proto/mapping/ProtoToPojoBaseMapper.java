package org.somda.sdc.proto.mapping;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.participant.AbstractDeviceComponentDescriptor;
import org.somda.sdc.biceps.model.participant.CodedValue;
import org.somda.sdc.biceps.model.participant.InstanceIdentifier;
import org.somda.sdc.biceps.model.participant.LocalizedText;
import org.somda.sdc.biceps.model.participant.OperatingJurisdiction;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.proto.model.biceps.AbstractDeviceComponentDescriptorMsg;
import org.somda.sdc.proto.model.biceps.CodedValueMsg;
import org.somda.sdc.proto.model.biceps.InstanceIdentifierMsg;
import org.somda.sdc.proto.model.biceps.LocalizedTextMsg;
import org.somda.sdc.proto.model.biceps.OperatingJurisdictionMsg;

import javax.annotation.Nullable;
import java.util.stream.Collectors;

public class ProtoToPojoBaseMapper {
    private static final Logger LOG = LogManager.getLogger(ProtoToPojoNodeMapper.class);
    private final Logger instanceLogger;

    @Inject
    ProtoToPojoBaseMapper(@Named(org.somda.sdc.common.CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
    }

    public InstanceIdentifier map(@Nullable InstanceIdentifierMsg protoMsg) {
        if (protoMsg == null) {
            return null;
        }

        var pojo = new InstanceIdentifier();
        pojo.setExtensionName(Util.optionalStr(protoMsg, "AExtension"));
        if (protoMsg.hasARoot() && !protoMsg.getARoot().getValue().isEmpty()) {
            pojo.setRootName(protoMsg.getARoot().getValue());
        }
        pojo.setType(map(Util.optional(protoMsg, "Type", CodedValueMsg.class)));
        pojo.setIdentifierName(protoMsg.getIdentifierNameList().stream().map(this::map).collect(Collectors.toList()));
        return pojo;
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
        pojo.setCode(protoMsg.getACode());
        if (protoMsg.hasACodingSystem() && !protoMsg.getACodingSystem().getValue().isEmpty()) {
            pojo.setCodingSystem(protoMsg.getACodingSystem().getValue());
        } else {
            pojo.setCodingSystem("urn:oid:1.2.840.10004.1.1.1.0.0.1");
        }
        pojo.setCodingSystem(Util.optionalStr(protoMsg, "ACodingSystemVersion"));
        pojo.setSymbolicCodeName(Util.optionalStr(protoMsg, "ASymbolicCodeName"));
        return pojo;
    }

    public LocalizedText map(@Nullable LocalizedTextMsg protoMsg) {
        if (protoMsg == null) {
            return null;
        }

        var pojo = new LocalizedText();
        pojo.setLang(Util.optionalStr(protoMsg, "ALang"));
        pojo.setRef(Util.optionalStr(protoMsg, "ARef"));
        pojo.setValue(protoMsg.getString());
        pojo.setVersion(Util.optionalBigIntOfLong(protoMsg, "AVersion"));
        return pojo;
    }

    public AbstractDeviceComponentDescriptor.ProductionSpecification map(
            @Nullable AbstractDeviceComponentDescriptorMsg.ProductionSpecificationMsg protoMsg) {
        if (protoMsg == null) {
            return null;
        }

        var pojo = new AbstractDeviceComponentDescriptor.ProductionSpecification();
        pojo.setProductionSpec(protoMsg.getProductionSpec());
        pojo.setSpecType(map(Util.optional(protoMsg, "SpecType", CodedValueMsg.class)));
        pojo.setComponentId(map(Util.optional(protoMsg, "ComponentId", InstanceIdentifierMsg.class)));

        return pojo;
    }
}
