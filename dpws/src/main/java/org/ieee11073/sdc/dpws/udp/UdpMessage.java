package org.ieee11073.sdc.dpws.udp;

/**
 * Raw UDP message packed as byte array plus length.
 */
public class UdpMessage {
    private final int length;
    private final String host;
    private final Integer port;
    private final byte[] data;

    public UdpMessage(byte[] data, int length, String host, Integer port) {
        this.data = data;
        this.length = length;
        this.host = host;
        this.port = port;
    }

    public UdpMessage(byte[] data, int length) {
        this.data = data;
        this.length = length;
        this.host = null;
        this.port = null;
    }

    boolean hasTransportData() {
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
     * Get data byte message.
     *
     * Do not rely on the byte array's length attribute, retrieve the length via {@link #getLength()} instead.
     */
    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        return new String(data);
    }
}
