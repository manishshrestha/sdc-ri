package org.somda.sdc.proto.mapping;

import org.somda.sdc.biceps.model.participant.AbstractMetricDescriptor;
import org.somda.sdc.biceps.model.participant.CauseInfo;
import org.somda.sdc.biceps.model.participant.CodedValue;
import org.somda.sdc.biceps.model.participant.InstanceIdentifier;
import org.somda.sdc.biceps.model.participant.LocalizedText;
import org.somda.sdc.biceps.model.participant.LocalizedTextWidth;
import org.somda.sdc.biceps.model.participant.Measurement;
import org.somda.sdc.biceps.model.participant.PhysicalConnectorInfo;
import org.somda.sdc.biceps.model.participant.Range;
import org.somda.sdc.biceps.model.participant.RemedyInfo;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;


/**
 * Collection of types with all fields/attributes set, for use in other round trips
 */
public class TypeCollection {

    public static final LocalizedText LOCALIZED_TEXT = new LocalizedText();
    static {
        LOCALIZED_TEXT.setRef("reflcopter");
        LOCALIZED_TEXT.setLang("tlh"); // Klingon, tlhIngan Hol, tlhIngan-Hol :)
        LOCALIZED_TEXT.setVersion(new BigInteger("1967"));
        LOCALIZED_TEXT.setTextWidth(LocalizedTextWidth.XS);
        LOCALIZED_TEXT.setValue("nuqneH");
    }

    public static final CodedValue.Translation CODED_VALUE_TRANSLATION = new CodedValue.Translation();
    static {
        CODED_VALUE_TRANSLATION.setCode("Z̮̞̠͙͔ͅḀ̗̞͈̻̗Ḷ͙͎̯̹̞͓G̻O̭̗̮");
        CODED_VALUE_TRANSLATION.setCodingSystem("http://a/b/c/g;x?y#s");
        CODED_VALUE_TRANSLATION.setCodingSystemVersion("¯\\_(ツ)_/");

        // TODO: Extension
//        CODED_VALUE_TRANSLATION.setExtension();
    }

    public static final CodedValue CODED_VALUE = new CodedValue();
    static {
        CODED_VALUE.setCode("ヽ༼ຈل͜ຈ༽ﾉ");
        CODED_VALUE.setCodingSystem("http://a/b/c/g;x?y#s");
        CODED_VALUE.setCodingSystemVersion("¯\\_(ツ)_/");
        CODED_VALUE.setSymbolicCodeName("Ｔｈｅ ｑｕｉｃｋ ｂｒｏｗｎ"); // fullwidth characters, fullwidth fun

        CODED_VALUE.setCodingSystemName(List.of(LOCALIZED_TEXT, LOCALIZED_TEXT));
        CODED_VALUE.setConceptDescription(List.of(LOCALIZED_TEXT));
        CODED_VALUE.setTranslation(List.of(CODED_VALUE_TRANSLATION));

        // TODO: Extension
//        CODED_VALUE.setExtension();
    }

    public static final RemedyInfo REMEDY_INFO = new RemedyInfo();
    static {
        REMEDY_INFO.setDescription(List.of(LOCALIZED_TEXT, LOCALIZED_TEXT, LOCALIZED_TEXT));

        // TODO: Extension
//        REMEDY_INFO.setExtension();
    }

    public static final CauseInfo CAUSE_INFO = new CauseInfo();
    static {
        CAUSE_INFO.setRemedyInfo(REMEDY_INFO);
        CAUSE_INFO.setDescription(List.of(LOCALIZED_TEXT, LOCALIZED_TEXT));

        // TODO: Extension
//        CAUSE_INFO.setExtension();
    }

    public static final Range RANGE = new Range();
    static {
        RANGE.setLower(BigDecimal.valueOf(12));
        RANGE.setUpper(BigDecimal.valueOf(15));
        RANGE.setStepWidth(BigDecimal.valueOf(0.5));
        RANGE.setRelativeAccuracy(BigDecimal.valueOf(0.3));
        RANGE.setAbsoluteAccuracy(BigDecimal.valueOf(0.1));

        // TODO: Extension
//        RANGE.setExtension();
    }

    public static final InstanceIdentifier INSTANCE_IDENTIFIER = new InstanceIdentifier();
    static {
        INSTANCE_IDENTIFIER.setRootName("http://a/b/c/g;x?y#s");
        INSTANCE_IDENTIFIER.setExtensionName("Wroooooooom");
        INSTANCE_IDENTIFIER.setType(CODED_VALUE);
        INSTANCE_IDENTIFIER.setIdentifierName(List.of(LOCALIZED_TEXT, LOCALIZED_TEXT, LOCALIZED_TEXT));

        // TODO: Extension
//        INSTANCE_IDENTIFIER.setExtension();
    }

    public static final AbstractMetricDescriptor.Relation RELATION = new AbstractMetricDescriptor.Relation();
    static {
        RELATION.setKind(AbstractMetricDescriptor.Relation.Kind.OTH);
        RELATION.setEntries(List.of("\uD83E\uDD93", "\uD83D\uDC0E"));
        RELATION.setCode(CODED_VALUE);
        RELATION.setIdentification(INSTANCE_IDENTIFIER);

        // TODO: Extension
//        RELATION.setExtension();
    }

    public static final PhysicalConnectorInfo PHYSICAL_CONNECTOR_INFO = new PhysicalConnectorInfo();
    static {
        PHYSICAL_CONNECTOR_INFO.setNumber(1);
        PHYSICAL_CONNECTOR_INFO.setLabel(List.of(LOCALIZED_TEXT, LOCALIZED_TEXT));

        // TODO: Extension
//        PHYSICAL_CONNECTOR_INFO.setExtension();
    }

    public static final Measurement MEASUREMENT = new Measurement();
    static {
        MEASUREMENT.setMeasuredValue(BigDecimal.valueOf(555555555));
        MEASUREMENT.setMeasurementUnit(CODED_VALUE);
    }

}
