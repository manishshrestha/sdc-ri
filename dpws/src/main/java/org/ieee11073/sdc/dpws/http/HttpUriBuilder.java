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
     * Create a URI with a randomized port in accordance to the configured minimum and maximum.
     *
     * The implied port range is [8000,8079]
     *
     * Note: for now only HTTP is supported.
     * TODO: 22.09.2016 Support for HTTPS
     *
     * @param host The host to insert into the URI.
     * @param port The port to insert into the URI.
     */
    public URI buildUri(String host, int port) {
        return URI.create("http://" + host + ":" + port);
    }
}
