package org.ieee11073.sdc.dpws.soap.wseventing.helper;

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

    public Optional<URI> getIdentifier() {
        return Optional.ofNullable(identifier);
    }

    public void setIdentifier(@Nullable URI identifier) {
        this.identifier = identifier;
    }
}
