package org.ieee11073.sdc.dpws.device.helper;

/**
 * URI schemes that are supported to be used as an EPR address.
 *
 * The recommended way of creating a unique EPR address is the UUID approach. A comprehensive list of URI schemes is
 * requestable from the <a href="https://www.iana.org/assignments/uri-schemes/uri-schemes.xhtml">IANA website</a>.
 *
 * The device logic may use portions of the EPR address information to enrich XAddrs URL
 * base paths. If an unknown URI type is given, no extra information is supposed to be added.
 */
public enum SupportedEprUriScheme {
    /**
     * HTTP URI scheme.
     *
     * @see <a href="http://www.iana.org/go/rfc8615">[RFC8615]</a>
     */
    HTTP("http", ""),

    /**
     * HTTPS URI scheme.
     *
     * @see <a href="http://www.iana.org/go/rfc8615">[RFC8615]</a>
     */
    HTTPS("https", ""),

    /**
     * URN URI scheme with namespace _uuid_.
     *
     * @see <a href="https://tools.ietf.org/html/rfc8141">[RFC8141]</a>
     * @see <a href="https://www.iana.org/assignments/urn-namespaces/urn-namespaces.xhtml">URN namespaces</a>
     */
    URN_UUID("urn", "uuid"),

    /**
     * URN URI scheme with namespace _oid_.
     *
     * @see <a href="https://tools.ietf.org/html/rfc8141">[RFC8141]</a>
     * @see <a href="https://www.iana.org/assignments/urn-namespaces/urn-namespaces.xhtml">URN namespaces</a>
     */
    URN_OID("urn", "oid");

    private String schemeName;
    private String specificPart;

    SupportedEprUriScheme(String schemeName, String specificPart) {
        this.schemeName = schemeName;
        this.specificPart = specificPart;
    }

    public String getSchemeName() {
        return schemeName;
    }

    public String getSpecificPart() {
        return specificPart;
    }

}
