package org.ieee11073.sdc.dpws.soap;

/**
 * Utility class to provide local transport information.
 */
public class TransportInfo {
    private final String scheme;
    private final String localAddress;
    private final int localPort;

    public TransportInfo(String scheme, String localAddress, int localPort) {
        this.scheme = scheme;
        this.localAddress = localAddress;
        this.localPort = localPort;
    }

    public String getLocalAddress() {
        return localAddress;
    }

    public int getLocalPort() {
        return localPort;
    }

    public String getScheme() {
        return scheme;
    }
}
