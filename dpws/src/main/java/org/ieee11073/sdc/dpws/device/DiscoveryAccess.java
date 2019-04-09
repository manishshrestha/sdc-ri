package org.ieee11073.sdc.dpws.device;

import javax.xml.namespace.QName;
import java.net.URI;
import java.util.List;

/**
 * Interface to access discovery functionality provided by a {@link Device}.
 */
public interface DiscoveryAccess {
    void setTypes(List<QName> types);

    void setScopes(List<URI> scopes);
}
