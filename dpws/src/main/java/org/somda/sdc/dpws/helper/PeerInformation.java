package org.somda.sdc.dpws.helper;

import java.net.URI;

/**
 * Remote and local address provision of a network peer.
 */
public class PeerInformation {
    private final URI remoteAddress;
    private final String localHost;

    /**
     * Creates instance with given remote address and local hostname/address.
     *
     * @param remoteAddress the remote address of the remote peer.
     * @param localHost the local hostname/address that is used to communicate with remoteAddress.
     */
    public PeerInformation(URI remoteAddress, String localHost) {
        this.remoteAddress = remoteAddress;
        this.localHost = localHost;
    }

    public URI getRemoteAddress() {
        return remoteAddress;
    }

    public String getLocalHost() {
        return localHost;
    }

    /**
     * Compares two peer objects with each other.
     *
     * @param o the object to compare against.
     * @return true if the remote address and local hostname/address are equal, otherwise false.
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
