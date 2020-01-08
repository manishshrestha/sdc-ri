package org.somda.sdc.glue.common;

import org.somda.sdc.biceps.model.participant.AbstractComplexDeviceComponentDescriptor;
import org.somda.sdc.biceps.model.participant.CodedValue;
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

    private static final Pattern pattern = Pattern.compile("^sdc\\.cdc\\.type\\:\\/" +
                    "(?<codingsystem>.*?)\\/" +
                    "(?<codingsystemversion>.*?)\\/" +
                    "(?<code>.*?)$",
            Pattern.CASE_INSENSITIVE);

    /**
     * Maps an abstract complex component descriptor to URI representation.
     *
     * @param descriptor the device component where to access the type.
     * @return the mapped URI or {@linkplain Optional#empty()} if type of descriptor was null.
     */
    public static Optional<URI> fromComplexDeviceComponent(AbstractComplexDeviceComponentDescriptor descriptor) {
        final CodedValue codedValue = descriptor.getType();
        if (codedValue == null) {
            return Optional.empty();
        }

        String codingSystem = codedValue.getCodingSystem();
        if ("urn:oid:1.2.840.10004.1.1.1.0.0.1".equals(codingSystem)) {
            codingSystem = null;
        }

        return Optional.of(URI.create(SCHEME + ":" +
                "/" + UrlUtf8.encode(codingSystem) +
                "/" + UrlUtf8.encode(codedValue.getCodingSystemVersion()) +
                "/" + UrlUtf8.encode(codedValue.getCode())));
    }

    /**
     * Maps a complex device component type URI to coded value.
     *
     * @param complexDeviceComponentTypeUri the URI to parse.
     * @return a coded value if pattern of URI matches or {@linkplain Optional#empty()} otherwise.
     */
    public static Optional<CodedValue> fromUri(URI complexDeviceComponentTypeUri) {
        Matcher matcher = pattern.matcher(complexDeviceComponentTypeUri.toString());
        if (matcher.matches()) {
            final String codingSystem = UrlUtf8.decode(matcher.group("codingsystem"));
            final String codingSystemVersion = UrlUtf8.decode(matcher.group("codingsystemversion"));
            final String code = UrlUtf8.decode(matcher.group("code"));
            CodedValue codedValue = new CodedValue();
            codedValue.setCodingSystem(codingSystem.isEmpty() ? null : codingSystem);
            codedValue.setCodingSystemVersion(codingSystemVersion.isEmpty() ? null : codingSystemVersion);
            codedValue.setCode(code.isEmpty() ? null : code);
            return Optional.of(codedValue);
        }

        return Optional.empty();
    }
}
