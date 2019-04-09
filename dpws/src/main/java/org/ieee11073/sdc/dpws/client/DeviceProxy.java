package org.ieee11073.sdc.dpws.client;

import javax.xml.namespace.QName;
import java.net.URI;
import java.util.List;

/**
 * Device information that is resolved through WS-Discovery.
 *
 * @see <a href="http://docs.oasis-open.org/ws-dd/discovery/1.1/wsdd-discovery-1.1-spec.html">WS-Discovery</a>
 */
public class DeviceProxy {
    private final URI eprAddress;
    private final List<QName> types;
    private final List<String> scopes;
    private final List<String> xAddrs;
    private final long metadataVersion;

    public DeviceProxy(URI eprAddress,
                       List<QName> types,
                       List<String> scopes,
                       List<String> xAddrs,
                       long metadataVersion) {
        this.eprAddress = eprAddress;
        this.types = types;
        this.scopes = scopes;
        this.xAddrs = xAddrs;
        this.metadataVersion = metadataVersion;
    }

    public URI getEprAddress() {
        return eprAddress;
    }

    public List<QName> getTypes() {
        return types;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public List<String> getXAddrs() {
        return xAddrs;
    }

    public long getMetadataVersion() {
        return metadataVersion;
    }
}
