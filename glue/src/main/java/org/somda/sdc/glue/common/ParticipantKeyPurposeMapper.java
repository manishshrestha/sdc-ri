package org.somda.sdc.glue.common;

import org.ietf.jgss.GSSException;
import org.ietf.jgss.Oid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Optional;

/**
 * Utility class to map between Participant Key Purpose URIs and OIDs.
 * <p>
 * This class implements the grammar defined in IEEE 11073-20701 section 9.3.
 */
public class ParticipantKeyPurposeMapper {
    private static final Logger LOG = LoggerFactory.getLogger(ParticipantKeyPurposeMapper.class);
    private static final String SCHEME = "sdc.mds.pkp";

    /**
     * Creates a Participant Key Purpose URI out of an OID.
     *
     * @param oid the OID to convert.
     * @return the converted URI.
     */
    public static URI fromOid(Oid oid) {
        return URI.create(SCHEME + ":" + oid.toString());
    }

    /**
     * Creates an OID given a Participant Key Purpose encoded URI.
     *
     * @param uri the URI to convert.
     * @return the converted OID or {@link Optional#empty()} if something went wrong.
     */
    public static Optional<Oid> fromUri(URI uri) {
        if (!uri.getScheme().equals(SCHEME)) {
            LOG.info("Unrecognized URI scheme. Expected '{}', actual is '{}'", SCHEME, uri.getScheme());
            return Optional.empty();
        }

        try {
            Oid oid = new Oid(uri.getSchemeSpecificPart());
            return Optional.of(oid);
        } catch (GSSException e) {
            LOG.info("Received malformed participant key purpose URI: {}", uri);
            return Optional.empty();
        }
    }
}
