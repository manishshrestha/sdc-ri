package org.somda.sdc.dpws.factory;

import org.somda.sdc.dpws.TransportBinding;

import java.net.URI;

/**
 * Factory to create {@linkplain TransportBinding} instances.
 * <p>
 * {@link TransportBinding} instances can be used to request any DPWS compatible endpoints.
 */
public interface TransportBindingFactory {

    /**
     * Generic factory method to create a binding on the basis of a URI.
     * <p>
     * Please note that - depending on the implementation - bindings are not supported. In this case an
     * {@link UnsupportedOperationException} is thrown.
     *
     * @param endpointUri the URI to create a binding to.
     * @return a transport binding bound to endpointUri.
     * @throws UnsupportedOperationException if the URI scheme is not supported.
     */
    TransportBinding createTransportBinding(URI endpointUri) throws UnsupportedOperationException;

    /**
     * Creates an HTTP or HTTPS binding.
     *
     * @param endpointUri a valid HTTP/HTTPS URI to create a binding to.
     * @return a transport binding bound to endpointUri.
     * @throws UnsupportedOperationException if the URI scheme type is not supported.
     */
    TransportBinding createHttpBinding(URI endpointUri) throws UnsupportedOperationException;
}
