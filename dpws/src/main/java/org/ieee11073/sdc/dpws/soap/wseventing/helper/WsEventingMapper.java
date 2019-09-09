package org.ieee11073.sdc.dpws.soap.wseventing.helper;

import com.google.inject.Inject;
import org.ieee11073.sdc.dpws.soap.wseventing.WsEventingConstants;
import org.ieee11073.sdc.dpws.soap.wseventing.model.ObjectFactory;
import org.ieee11073.sdc.common.helper.JaxbUtil;

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

    public void mapToJaxbSoapHeader(WsEventingHeader src, List<Object> dest) {
        src.getIdentifier().ifPresent(uri -> dest.add(wseFactory.createIdentifier(uri.toString())));
    }

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
