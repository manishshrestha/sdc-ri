package org.ieee11073.sdc.dpws.helper;

import java.net.URI;

/**
 * Remote and local address provision of a network peer.
 */
public class PeerInformation {
    private final URI remoteAddress;
    private final String localHost;

    /**
     * Create instance with given remote address and local hostname/address.
     * @param remoteAddress The remote address of the remote peer.
     * @param localHost The local hostname/address that is used to communicate with remoteAddress.
     */
    public PeerInformation(URI remoteAddress, String localHost) {
        this.remoteAddress = remoteAddress;
        this.localHost = localHost;
    }

    /**
     * Get remote address of peer connection.
     */
    public URI getRemoteAddress() {
        return remoteAddress;
    }

    /**
     * Get local hostname/address of peer connection.
     */
    public String getLocalHost() {
        return localHost;
    }

    /**
     * Compare two peer objects to equality.
     *
     * @param o The object to compare against.
     * @return Return true if remote address and local hostname/address are equal, otherwise false.
     */
    @Override
    public boolean equals(Object o) {
        if (o == null || !o.getClass().equals(getClass())) {
            return false;
        }

        if (o == this) {
            return true;
        }

        PeerInformation castedObj = (PeerInformation) o;
        return castedObj.remoteAddress.equals(remoteAddress) && castedObj.localHost.equals(localHost);
    }
}
