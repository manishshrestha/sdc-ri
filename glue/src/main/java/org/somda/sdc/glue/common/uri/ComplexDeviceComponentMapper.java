package org.somda.sdc.glue.common.uri;

import org.somda.sdc.biceps.model.participant.AbstractComplexDeviceComponentDescriptor;
import org.somda.sdc.biceps.model.participant.CodedValue;
import org.somda.sdc.glue.GlueConstants;
import org.somda.sdc.glue.common.helper.UrlUtf8;

import java.net.URI;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to map from complex device component coded value to URI and back to coded value.
 * <p>
 * This class implements the grammar defined in IEEE 11073-20701 section 9.2.
 */
public class ComplexDeviceComponentMapper {
    private static final String SCHEME = "sdc.cdc.type";

    private static final Pattern PATTERN = Pattern
            .compile(
                    "^(" +
                            "(?i:sdc.cdc.type):/" +
                            // Warning: this intentionally differs from
                            // the standard to distinguish between authority and path
                            // Do not change the first of the following groups to SEGMENT_REGEX instead
                            // of SEGMENT_NZ_REGEX
                            "(?<nonAuthorityCodingsystem>" + GlueConstants.SEGMENT_NZ_REGEX + ")/" +
                            "(?<nonAuthorityCodingsystemVersion>" + GlueConstants.SEGMENT_REGEX + ")/" +
                            "(?<nonAuthorityCode>" + GlueConstants.SEGMENT_NZ_REGEX + ")|" +
                            "((?i:sdc.cdc.type)://" +
                            "(?<authorityCodingsystemVersion>(" + GlueConstants.AUTHORITY + "))/" +
                            "(?<authorityCode>" + GlueConstants.SEGMENT_NZ_REGEX + ")" +
                            ")" +
                            ")$"
            );

    /**
     * Maps an abstract complex component descriptor to URI representation.
     *
     * @param descriptor the device component where to access the type.
     * @return the mapped URI or {@linkplain Optional#empty()} if type of descriptor was null.
     */
    public static URI fromComplexDeviceComponent(AbstractComplexDeviceComponentDescriptor descriptor)
            throws UriMapperGenerationArgumentException {
        final CodedValue codedValue = descriptor.getType();
        if (codedValue == null) {
            throw new UriMapperGenerationArgumentException("No CodedValue was provided.");
        }

        return fromCodedValue(codedValue);
    }

    /**
     * Given a coded value that belongs to an abstract complex component descriptor,
     * this function creates the URI representation.
     *
     * @param codedValue a complex device component's type.
     * @return the mapped URI.
     */
    public static URI fromCodedValue(CodedValue codedValue) throws UriMapperGenerationArgumentException {
        String codingSystem = codedValue.getCodingSystem();
        if ("urn:oid:1.2.840.10004.1.1.1.0.0.1".equals(codingSystem)) {
            codingSystem = null;
        }

        URI uri = URI.create(SCHEME + ":" +
                "/" + UrlUtf8.encode(codingSystem) +
                "/" + UrlUtf8.encode(codedValue.getCodingSystemVersion()) +
                "/" + UrlUtf8.encode(codedValue.getCode()));

        try {
            fromUri(uri);
        } catch (UriMapperParsingException e) {
            throw new UriMapperGenerationArgumentException(
                    "No valid URI could be generated from the given CodedValue instance.");
        }

        return uri;
    }

    /**
     * Maps a complex device component type URI to coded value.
     *
     * @param complexDeviceComponentTypeUri the URI to parse.
     * @return a coded value if pattern of URI matches or {@linkplain Optional#empty()} otherwise.
     */
    public static CodedValue fromUri(URI complexDeviceComponentTypeUri) throws UriMapperParsingException {

        Matcher matcher = PATTERN.matcher(complexDeviceComponentTypeUri.toString());
        if (matcher.matches()) {

            final String codingSystem;
            final String codingSystemVersion;
            final String code;

            if (matcher.group("nonAuthorityCode") != null) {
                codingSystem = UrlUtf8.decode(matcher.group("nonAuthorityCodingsystem"));
                codingSystemVersion = UrlUtf8.decode(matcher.group("nonAuthorityCodingsystemVersion"));
                code = UrlUtf8.decode(matcher.group("nonAuthorityCode"));
            } else {
                codingSystem = "";
                codingSystemVersion = UrlUtf8.decode(matcher.group("authorityCodingsystemVersion"));
                code = UrlUtf8.decode(matcher.group("authorityCode"));
            }


            CodedValue codedValue = new CodedValue();
            codedValue.setCodingSystem(codingSystem.isEmpty() ? null : codingSystem);
            codedValue.setCodingSystemVersion(codingSystemVersion.isEmpty() ? null : codingSystemVersion);
            codedValue.setCode(code.isEmpty() ? null : code);
            return codedValue;
        }

        throw new UriMapperParsingException(
                "Invalid URI for the mapper" + ComplexDeviceComponentMapper.class.toString() + ".");
    }
}
