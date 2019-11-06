package org.somda.sdc.dpws.soap.wseventing.helper;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Optional;

/**
 * Convenience class to represent WS-Eventing header information.
 *
 * @see <a href="https://www.w3.org/Submission/2006/SUBM-WS-Eventing-20060315/#Subscribe">Subscribe</a>
 */
public class WsEventingHeader {
    private URI identifier;

    /**
     * Gets the WS-Eventing identifier.
     *
     * @return the identifier if assigned, otherwise {@link Optional#empty()}.
     * @deprecated The WS-Eventing identifier is a non-conforming header item and should not be used.
     */
    public Optional<URI> getIdentifier() {
        return Optional.ofNullable(identifier);
    }

    /**
     * Sets the WS-Eventing identifier.
     *
     * @param identifier the identifier to set.
     * @deprecated The WS-Eventing identifier is a non-conforming header item and should not be used.
     */
    public void setIdentifier(@Nullable URI identifier) {
        this.identifier = identifier;
    }
}
