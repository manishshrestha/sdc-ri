package org.somda.sdc.dpws.udp;

import javax.annotation.Nullable;

/**
 * Raw UDP message packed as a byte array plus a length attribute and receiver information.
 */
public class UdpMessage {
    private final int length;
    private final String host;
    private final Integer port;
    private final byte[] data;

    /**
     * Constructor with transport information.
     *
     * @param data   the payload of the UDP message.
     * @param length the actual message length.
     * @param host   the message receiver's host.
     * @param port   the message receiver's port.
     */
    public UdpMessage(byte[] data, int length, @Nullable String host, @Nullable Integer port) {
        this.data = data;
        this.length = length;
        this.host = host;
        this.port = port;
    }

    /**
     * Constructor without transport information.
     * <p>
     * Transport information is not required in case this message is used with multicast.
     *
     * @param data   the payload of the UDP message.
     * @param length the actual message length.
     */
    public UdpMessage(byte[] data, int length) {
        this(data, length, null, null);
    }

    /**
     * Checks if there is transport data attached to the message.
     *
     * @return true if there is a host and port, otherwise false.
     */
    public boolean hasTransportData() {
        return host != null && port != null;
    }

    public Integer getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    public int getLength() {
        return length;
    }

    /**
     * Gets the data from this object as byte array.
     * <p>
     * <em>Do not rely on the byte array's length attribute, retrieve the length via {@link #getLength()} instead!</em>
     *
     * @return message byte array.
     */
    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        return new String(data, 0, length);
    }
}
