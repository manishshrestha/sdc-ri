package org.somda.sdc.dpws.device;

import javax.xml.namespace.QName;
import java.net.URI;
import java.util.Collection;

/**
 * Interface to set discovery metadata provided by a {@linkplain Device}.
 */
public interface DiscoveryAccess {
    /**
     * Sets the types in accordance with WS-Discovery.
     *
     * @param types the types to set.
     * @see <a href="http://docs.oasis-open.org/ws-dd/discovery/1.1/os/wsdd-discovery-1.1-spec-os.html#_Toc234231830">Matching Types and Scopes</a>
     */
    void setTypes(Collection<QName> types);

    /**
     * Sets the scopes in accordance with WS-Discovery.
     *
     * @param scopes the scopes to set.
     * @see <a href="http://docs.oasis-open.org/ws-dd/discovery/1.1/os/wsdd-discovery-1.1-spec-os.html#_Toc234231830">Matching Types and Scopes</a>
     */
    void setScopes(Collection<URI> scopes);
}