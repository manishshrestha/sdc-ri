package org.somda.sdc.dpws.soap;

import javax.annotation.Nullable;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Utility class to provide transport information.
 * <p>
 * Transport information comprises:
 * <ul>
 * <li>the transportation scheme (e.g., soap-over-udp, http, https)
 * <li>local address and port information (if available)
 * <li>remote address and port information (if available)
 * <li>Transport layer certificates (if available)
 * </ul>
 */
public class TransportInfo {
    private final String scheme;
    private final String localAddress;
    private final Integer localPort;
    private final String remoteAddress;
    private final Integer remotePort;
    private List<X509Certificate> x509Certificates;

    @Deprecated(since = "1.1.0", forRemoval = true)
    public TransportInfo(String scheme,
                         @Nullable String localAddress,
                         @Nullable Integer localPort,
                         @Nullable String remoteAddress,
                         @Nullable Integer remotePort,
                         Collection<X509Certificate> x509Certificates) {
        this.scheme = scheme;
        this.localAddress = localAddress;
        this.localPort = localPort;
        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;
        // convert provided collection into a list
        this.x509Certificates = new ArrayList<>();
        this.x509Certificates.addAll(x509Certificates);
    }

    public TransportInfo(String scheme,
                         @Nullable String localAddress,
                         @Nullable Integer localPort,
                         @Nullable String remoteAddress,
                         @Nullable Integer remotePort,
                         List<X509Certificate> x509Certificates) {
        this.scheme = scheme;
        this.localAddress = localAddress;
        this.localPort = localPort;
        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;
        this.x509Certificates = x509Certificates;
    }

    /**
     * Gets the scheme that identifies the context of this transport information.
     *
     * @return the scheme; always present.
     */
    public String getScheme() {
        return scheme;
    }

    /**
     * Returns the local address used on the transport layer.
     *
     * @return the local address or {@linkplain Optional#empty()} if none is available.
     */
    public Optional<String> getLocalAddress() {
        return Optional.ofNullable(localAddress);
    }

    /**
     * Returns the local port used on the transport layer.
     *
     * @return the local port or {@linkplain Optional#empty()} if none is available.
     */
    public Optional<Integer> getLocalPort() {
        return Optional.ofNullable(localPort);
    }

    /**
     * Returns the remote address used on the transport layer.
     *
     * @return the remote address or {@linkplain Optional#empty()} if none is available.
     */
    public Optional<String> getRemoteAddress() {
        return Optional.ofNullable(remoteAddress);
    }

    /**
     * Returns the remote port used on the transport layer.
     *
     * @return the remote port or {@linkplain Optional#empty()} if none is available.
     */
    public Optional<Integer> getRemotePort() {
        return Optional.ofNullable(remotePort);
    }

    /**
     * Any transport-layer specific X509 certificates.
     *
     * @return a list of certificates that can be empty if no transport-layer security is activated.
     */
    public List<X509Certificate> getX509Certificates() {
        return x509Certificates;
    }
}
