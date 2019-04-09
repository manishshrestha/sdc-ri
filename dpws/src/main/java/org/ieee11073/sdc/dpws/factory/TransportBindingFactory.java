package org.ieee11073.sdc.dpws.factory;

import org.ieee11073.sdc.dpws.TransportBinding;

import java.net.URI;

/**
 * Factory to create {@link TransportBinding} instances.
 */
public interface TransportBindingFactory {

    /**
     * Generic factory method to create a binding on the basis of a URI.
     *
     * Please note that, depending on the implementation, some bindings are not supported. In this case an
     * @link UnsupportedOperationException} is thrown.
     *
     * @param endpointUri The URI to create a binding to.
     * @return A transport binding bound to endpointUri.
     * @throws UnsupportedOperationException If the URI scheme is not supported.
     */
    TransportBinding createTransportBinding(URI endpointUri) throws UnsupportedOperationException;

    /**
     * Create an HTTP or HTTPS binding.
     *
     * @param endpointUri A valid HTTP/HTTPS URI to create a binding to.
     * @return A transport binding bound to endpointUri.
     * @throws UnsupportedOperationException If the URI scheme type is not supported.
     */
    TransportBinding createHttpBinding(URI endpointUri) throws UnsupportedOperationException;
}
