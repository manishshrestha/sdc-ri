package org.somda.sdc.glue.common;

import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.glue.GlueConstants;
import org.somda.sdc.glue.common.helper.UrlUtf8;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
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
                    "(?i:(?<contextsource>sdc.ctxt.(loc|pat|ens|wfl|opr|mns))):/" +
                            "(?<root>" + GlueConstants.SEGMENT_NZ_REGEX + ")/" +
                            "(?<extension>" + GlueConstants.SEGMENT_REGEX + "$)"
            );

    /**
     * Converts from an instance identifier to an URI.
     *
     * @param instanceIdentifier the instance identifier to convert.
     * @param contextSource      the type of context to create a scheme for.
     * @return an URI that reflects the instance identifier.
     */
    public static URI fromInstanceIdentifier(InstanceIdentifier instanceIdentifier,
                                             ContextSource contextSource) {
        final String root = instanceIdentifier.getRootName() == null ? NULL_FLAVOR_ROOT : UrlUtf8.encode(instanceIdentifier.getRootName());
        final String extension = UrlUtf8.encode(instanceIdentifier.getExtensionName());
        return URI.create(contextSource.getSourceString() + ":/" + root + "/" + extension);
    }

    /**
     * Converts from an URI string to an instance identifier.
     *
     * @param contextIdentificationUri the URI to parse.
     * @param expectedContextSource    the expected context source.
     * @return the converted instance identifier or {@link Optional#empty()} if either there was a parsing error or
     * the scheme did not match the expected context source.
     */
    public static Optional<InstanceIdentifier> fromUri(String contextIdentificationUri,
                                                       ContextSource expectedContextSource) {
        Matcher matcher = PATTERN.matcher(contextIdentificationUri);
        if (matcher.matches()) {
            final String contextSource = matcher.group("contextsource");
            final String root = matcher.group("root");
            final String extension = matcher.group("extension");
            if (contextSource == null ||
                    root == null ||
                    extension == null ||
                    root.isEmpty() ||
                    !contextSource.equals(expectedContextSource.getSourceString())) {
                return Optional.empty();
            }

            try {
                final InstanceIdentifier instanceIdentifier = new InstanceIdentifier();
                final String decodedRoot = new URI(UrlUtf8.decode(root)).toString();
                instanceIdentifier.setRootName(decodedRoot.equals(NULL_FLAVOR_ROOT) ? null : decodedRoot);
                final String decodedExtension = UrlUtf8.decode(extension);
                instanceIdentifier.setExtensionName(decodedExtension.isEmpty() ? null : decodedExtension);
                return Optional.of(instanceIdentifier);
            } catch (URISyntaxException uriSyntaxException) {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    /**
     * Converts from an URI to an instance identifier.
     *
     * @param contextIdentificationUri the URI.
     * @param expectedContextSource    the context source.
     * @return the converted instance identifier.
     * @see #fromUri(String, ContextSource)
     */
    public static Optional<InstanceIdentifier> fromUri(URI contextIdentificationUri,
                                                       ContextSource expectedContextSource) {
        return fromUri(contextIdentificationUri.toString(), expectedContextSource);
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
