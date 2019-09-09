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
     * Shorthand method to create a {@link AttributedURIType} from simple string.
     */
    public AttributedURIType createAttributedURIType(String uri) {
        AttributedURIType attributedURIType = wsaFactory.createAttributedURIType();
        attributedURIType.setValue(uri);
        return attributedURIType;
    }

    public AttributedURIType createAttributedURIType(URI uri) {
        return createAttributedURIType(uri.toString());
    }

    public Optional<String> getAddressUriAsString(@Nullable EndpointReferenceType epr) {
        if (epr == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(epr.getAddress()).map(AttributedURIType::getValue);
    }

    public Optional<URI> getAddressUri(EndpointReferenceType epr) {
        Optional<String> addressUriAsString = getAddressUriAsString(epr);
        return addressUriAsString.map(URI::create);

    }

    public EndpointReferenceType createEprWithAddress(String addressUri) {
        EndpointReferenceType eprType = wsaFactory.createEndpointReferenceType();
        eprType.setAddress(createAttributedURIType(addressUri));
        return eprType;
    }

    public EndpointReferenceType createEprWithAddress(URI addressUri) {
        return createEprWithAddress(addressUri.toString());
    }
}
