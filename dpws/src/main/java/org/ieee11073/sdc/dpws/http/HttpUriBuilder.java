package org.ieee11073.sdc.dpws.http;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.net.URI;
import java.util.Random;

/**
 * Helper class to build valid HTTP(S) URIs.
 */
public class HttpUriBuilder {
    private final Integer portMin;
    private final Integer portMax;
    private final Random random;

    @Inject
    HttpUriBuilder(@Named(HttpConfig.PORT_MIN) Integer portMin,
                   @Named(HttpConfig.PORT_MAX) Integer portMax,
                   Random random) {
        this.portMin = portMin;
        this.portMax = portMax;
        this.random = random;
    }

    /**
     * Create a randomized port.
     *
     * The port range is defined in {@link HttpConfig}.
     *
     * @see HttpConfig#PORT_MIN
     * @see HttpConfig#PORT_MAX
     */
    public Integer buildRandomPort() {
        return random.nextInt(portMax - portMin + 1) + portMin;
    }

    /**
     * Create an HTTP URI with a randomized port in accordance to the configured minimum and maximum.
     *
     * @param scheme The scheme to insert into the URI.
     * @param host The host to insert into the URI.
     * @param port The port to insert into the URI.
     */
    public URI buildUri(String scheme, String host, int port) {
        return URI.create(scheme + "://" + host + ":" + port);
    }

    /**
     * Create an HTTP URI with given host and port.
     *
     * @param host The host to insert into the URI.
     * @param port The port to insert into the URI.
     */
    public URI buildUri(String host, int port) {
        return buildUri("http", host,  port);
    }

    /**
     * Create an HTTPS URI with given host an port.
     *
     * @param host The host to insert into the URI.
     * @param port The port to insert into the URI.
     */
    public URI buildSecuredUri(String host, int port) {
        return buildUri("https", host,  port);
    }
}
