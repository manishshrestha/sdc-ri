package org.somda.sdc.dpws.soap.wseventing.helper;

import com.google.inject.Inject;
import org.somda.sdc.dpws.soap.wseventing.WsEventingConstants;
import org.somda.sdc.dpws.soap.wseventing.model.ObjectFactory;
import org.somda.sdc.common.util.JaxbUtil;

import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * Mapper to map between WS-Addressing JAXB elements and {@link WsEventingHeader}.
 */
public class WsEventingMapper {
    private final JaxbUtil jaxbUtil;
    private final ObjectFactory wseFactory;

    @Inject
    WsEventingMapper(JaxbUtil jaxbUtil,
                     ObjectFactory wseFactory) {
        this.jaxbUtil = jaxbUtil;
        this.wseFactory = wseFactory;
    }

    /**
     * The mapper function that takes a convenience WS-Eventing header and adds it to a list of JAXB objects.
     *
     * @param src  the WS-Eventing source information.
     * @param dest the list of objects where to add the mapped JAXB element (typically the list of headers).
     */
    public void mapToJaxbSoapHeader(WsEventingHeader src, List<Object> dest) {
        src.getIdentifier().ifPresent(uri -> dest.add(wseFactory.createIdentifier(uri.toString())));
    }

    /**
     * The mapper function that takes a list of JAXB objects and populates the convenience WS-Eventing header.
     *
     * @param src  the list of objects where to get the WS-Discovery header information from.
     * @param dest the WS-Eventing mapper destination.
     */
    public void mapFromJaxbSoapHeader(List<Object> src, WsEventingHeader dest) {
        src.forEach(o -> {
            Optional<String> uri;

            uri = jaxbUtil.extractElement(o, WsEventingConstants.IDENTIFIER);
            if (uri.isPresent() && dest.getIdentifier().isEmpty()) {
                dest.setIdentifier(URI.create(uri.get()));
            }
        });
    }
}
