package org.somda.sdc.glue.common;

import org.somda.sdc.biceps.model.participant.*;

import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to map between context-based URI on the grammar in
 * <p>
 * This class implements the grammar defined in IEEE 11073-20701 section 9.4.
 */
public class ContextIdentificationMapper {
    private static final String NULL_FLAVOR_ROOT = "biceps.uri.unk";
    private static final String SCHEME_PREFIX = "sdc.ctxt.";

    private static final Pattern pattern = Pattern.compile("^(?<contextsource>sdc\\.ctxt\\..+?)\\:\\/" +
                    "(?<root>.*?)\\/" +
                    "(?<extension>.*?)$",
            Pattern.CASE_INSENSITIVE);

    /**
     * Converts from an instance identifier to an URI.
     *
     * @param instanceIdentifier the instance identifier to convert.
     * @param contextSource      the type of context to create a scheme for.
     * @return an URI that reflects the instance identifier.
     */
    public static URI fromInstanceIdentifier(InstanceIdentifier instanceIdentifier,
                                             ContextSource contextSource) {
        try {
            final String root = instanceIdentifier.getRootName() == null ? NULL_FLAVOR_ROOT : encode(instanceIdentifier.getRootName());
            final String extension = encode(instanceIdentifier.getExtensionName());
            return URI.create(contextSource.getSourceString() + ":/" + root + "/" + extension);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e); // this should never happen...
        }
    }

    /**
     * Converts from an URI string to an instance identifier.
     *
     * @param contextIdentificationUri the URI to parse.
     * @param expectedContextSource    the expected context source.
     * @return the converted instance identifier or {@link Optional#empty()} if either there was a parsing error or
     * the scheme did not match the expected context source.
     */
    public static Optional<InstanceIdentifier> fromURI(String contextIdentificationUri,
                                                       ContextSource expectedContextSource) {
        Matcher matcher = pattern.matcher(contextIdentificationUri);
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
                final String decodedRoot = new URI(decode(root)).toString();
                instanceIdentifier.setRootName(decodedRoot.equals(NULL_FLAVOR_ROOT) ? null : decodedRoot);
                final String decodedExtension = decode(extension);
                instanceIdentifier.setExtensionName(decodedExtension.isEmpty() ? null : decodedExtension);
                return Optional.of(instanceIdentifier);
            } catch (UnsupportedEncodingException encodingException) {
                throw new RuntimeException(encodingException); // this should never happen...
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
     * @see #fromURI(String, ContextSource)
     */
    public static Optional<InstanceIdentifier> fromURI(URI contextIdentificationUri,
                                                       ContextSource expectedContextSource) {
        return fromURI(contextIdentificationUri.toString(), expectedContextSource);
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

    private static String encode(@Nullable String text) throws UnsupportedEncodingException {
        return text == null ? "" : URLEncoder.encode(text, "UTF-8");
    }

    private static String decode(String text) throws UnsupportedEncodingException {
        return URLDecoder.decode(text, "UTF-8");
    }

}
