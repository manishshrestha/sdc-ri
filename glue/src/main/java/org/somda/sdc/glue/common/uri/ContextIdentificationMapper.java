package org.somda.sdc.glue.common.uri;

import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.glue.GlueConstants;
import org.somda.sdc.glue.common.helper.UrlUtf8;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to map between context-based URIs and instance identifiers.
 * <p>
 * This class implements the grammar defined in IEEE 11073-20701 section 9.4.
 */
public class ContextIdentificationMapper {
    private static final String NULL_FLAVOR_ROOT = "biceps.uri.unk";
    private static final String SCHEME_PREFIX = "sdc.ctxt.";

    private static final Pattern PATTERN = Pattern
            .compile(
                    "^(?i:(?<contextsource>sdc.ctxt.(loc|pat|ens|wfl|opr|mns))):/" +
                            "(?<root>" + GlueConstants.SEGMENT_NZ_REGEX + ")/" +
                            "(?<extension>" + GlueConstants.SEGMENT_REGEX + ")$"
            );

    /**
     * Converts from an instance identifier to an URI.
     *
     * @param instanceIdentifier the instance identifier to convert.
     * @param contextSource      the type of context to create a scheme for.
     * @return an URI that reflects the instance identifier.
     */
    public static String fromInstanceIdentifier(InstanceIdentifier instanceIdentifier,
                                                ContextSource contextSource)
            throws UriMapperGenerationArgumentException {
        final String root = instanceIdentifier.getRootName() == null ?
                NULL_FLAVOR_ROOT : UrlUtf8.encode(instanceIdentifier.getRootName());
        final String extension = UrlUtf8.encode(instanceIdentifier.getExtensionName());

        final String resultingUri = contextSource.getSourceString() + ":/" + root + "/" + extension;

        try {
            fromString(resultingUri, contextSource);
        } catch (UriMapperParsingException e) {
            throw new UriMapperGenerationArgumentException(
                    "No valid URI could be generated from the given instance identifier: " +
                            instanceIdentifier.toString() + " and context source: " + contextSource.toString());
        }

        return resultingUri;
    }

    /**
     * Converts from an URI string to an instance identifier.
     *
     * @param contextIdentificationUri the URI to parse.
     * @param expectedContextSource    the expected context source.
     * @return the converted instance identifier.
     */
    public static InstanceIdentifier fromString(String contextIdentificationUri,
                                                ContextSource expectedContextSource)
            throws UriMapperParsingException {

        Matcher matcher = PATTERN.matcher(contextIdentificationUri);

        if (matcher.matches()) {

            final String contextSource = matcher.group("contextsource");

            if (!expectedContextSource.getSourceString().equals(contextSource)) {
                throw new UriMapperParsingException(
                        "The expected context source: '" + expectedContextSource.getSourceString() +
                                "' does not match with the actual context source: '" + contextSource + "'" +
                        ContextIdentificationMapper.class.toString());
            }

            final String root = matcher.group("root");
            final String extension = matcher.group("extension");

            final InstanceIdentifier instanceIdentifier = new InstanceIdentifier();
            final String decodedRoot = UrlUtf8.decode(root);
            instanceIdentifier.setRootName(decodedRoot.equals(NULL_FLAVOR_ROOT) ? null : decodedRoot);
            final String decodedExtension = UrlUtf8.decode(extension);
            instanceIdentifier.setExtensionName(decodedExtension.isEmpty() ? null : decodedExtension);

            return instanceIdentifier;
        } else {
            throw new UriMapperParsingException("Invalid URI for the mapper " +
                    ContextIdentificationMapper.class.toString());
        }
    }

    /**
     * Defines the context instance identifier URI type.
     * <p>
     * This type reflects the scheme of the URI, which is according to the IEEE 11073-20701
     * <ul>
     * <li>sdc.ctxt.loc for {@link #Location}
     * <li>sdc.ctxt.pat for {@link #Patient}
     * <li>sdc.ctxt.ens for {@link #Ensemble}
     * <li>sdc.ctxt.wfl for {@link #Workflow}
     * <li>sdc.ctxt.opr for {@link #Operator}
     * <li>sdc.ctxt.mns for {@link #Means}
     * </ul>
     */
    public enum ContextSource {
        Location(SCHEME_PREFIX + "loc", LocationContextState.class),
        Patient(SCHEME_PREFIX + "pat", PatientContextState.class),
        Ensemble(SCHEME_PREFIX + "ens", EnsembleContextState.class),
        Workflow(SCHEME_PREFIX + "wfl", WorkflowContextState.class),
        Operator(SCHEME_PREFIX + "opr", OperatorContextState.class),
        Means(SCHEME_PREFIX + "mns", MeansContextState.class);

        private final String sourceString;
        private final Class<? extends AbstractContextState> sourceClass;

        ContextSource(String sourceString, Class<? extends AbstractContextState> sourceClass) {
            this.sourceString = sourceString;
            this.sourceClass = sourceClass;
        }

        public String getSourceString() {
            return sourceString;
        }

        public Class<? extends AbstractContextState> getSourceClass() {
            return sourceClass;
        }
    }
}
