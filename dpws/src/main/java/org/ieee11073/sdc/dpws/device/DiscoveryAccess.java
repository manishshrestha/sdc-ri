package org.ieee11073.sdc.dpws.device;

import javax.xml.namespace.QName;
import java.net.URI;
import java.util.List;

/**
 * Interface to set discovery metadata provided by a {@link Device}.
 */
public interface DiscoveryAccess {
    /**
     * Types in accordance with WS-Discovery.
     *
     * @see <a href="http://docs.oasis-open.org/ws-dd/discovery/1.1/os/wsdd-discovery-1.1-spec-os.html#_Toc234231830">Matching Types and Scopes</a>
     */
    void setTypes(List<QName> types);

    /**
     * Scopes in accordance with WS-Discovery.
     *
     * @see <a href="http://docs.oasis-open.org/ws-dd/discovery/1.1/os/wsdd-discovery-1.1-spec-os.html#_Toc234231830">Matching Types and Scopes</a>
     */
    void setScopes(List<URI> scopes);
}
