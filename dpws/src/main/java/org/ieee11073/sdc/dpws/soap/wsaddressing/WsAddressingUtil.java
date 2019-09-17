package org.ieee11073.sdc.dpws.soap.wsaddressing;

import com.google.inject.Inject;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.AttributedURIType;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.ObjectFactory;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Optional;

/**
 * Utility class for WS-Addressing related interaction.
 */
public class WsAddressingUtil {
    private final ObjectFactory wsaFactory;

    @Inject
    WsAddressingUtil(ObjectFactory wsaFactory) {
        this.wsaFactory = wsaFactory;
    }

    /**
     * Shorthand method to create an {@link AttributedURIType} from a simple string.
     *
     * @param uri the URI as string.
     * @return an {@link AttributedURIType} instance.
     */
    public AttributedURIType createAttributedURIType(String uri) {
        AttributedURIType attributedURIType = wsaFactory.createAttributedURIType();
        attributedURIType.setValue(uri);
        return attributedURIType;
    }

    /**
     * Shorthand method to create an {@link AttributedURIType} from an URI.
     *
     * @param uri the URI.
     * @return an {@link AttributedURIType} instance.
     */
    public AttributedURIType createAttributedURIType(URI uri) {
        return createAttributedURIType(uri.toString());
    }

    /**
     * Gets the address URI of an endpoint reference as string.
     *
     * @param epr the endpoint reference.
     * @return the endpoint reference or {@linkplain Optional#empty()} if there was no URI available.
     */
    public Optional<String> getAddressUriAsString(EndpointReferenceType epr) {
        return Optional.ofNullable(epr.getAddress()).map(AttributedURIType::getValue);
    }

    /**
     * Gets the address URI of an endpoint reference.
     *
     * @param epr the endpoint reference.
     * @return the endpoint reference or {@linkplain Optional#empty()} if there was no URI available.
     */
    public Optional<URI> getAddressUri(EndpointReferenceType epr) {
        Optional<String> addressUriAsString = getAddressUriAsString(epr);
        return addressUriAsString.map(URI::create);
    }

    /**
     * Creates an endpoint reference given an address string.
     *
     * @param addressUri the address string of the endpoint reference.
     * @return a new endpoint reference object.
     */
    public EndpointReferenceType createEprWithAddress(String addressUri) {
        EndpointReferenceType eprType = wsaFactory.createEndpointReferenceType();
        eprType.setAddress(createAttributedURIType(addressUri));
        return eprType;
    }

    /**
     * Creates an endpoint reference given an address URI.
     *
     * @param addressUri the address URI of the endpoint reference.
     * @return a new endpoint reference object.
     */
    public EndpointReferenceType createEprWithAddress(URI addressUri) {
        return createEprWithAddress(addressUri.toString());
    }
}
