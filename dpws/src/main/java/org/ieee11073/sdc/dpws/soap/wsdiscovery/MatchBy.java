package org.ieee11073.sdc.dpws.soap.wsdiscovery;

/**
 * Set of supported MatchBy rules for WS-Addressing Scopes type.
 *
 * Fulfill at least dpws:R1019.
 */
public enum MatchBy {
    RFC3986("http://docs.oasis-open.org/ws-dd/ns/discovery/2009/01/rfc3986"),
    STRCMP0("http://docs.oasis-open.org/ws-dd/ns/discovery/2009/01/strcmp0");

    MatchBy(String matchByUri) {
        this.matchByUri = matchByUri;
    }

    public String getUri() {
        return matchByUri;
    }

    private final String matchByUri;
}
