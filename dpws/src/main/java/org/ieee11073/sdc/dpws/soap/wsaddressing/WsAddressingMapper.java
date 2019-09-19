package org.ieee11073.sdc.dpws.soap.wsaddressing;

import com.google.inject.Inject;
import org.ieee11073.sdc.common.helper.JaxbUtil;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.AttributedURIType;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.ObjectFactory;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.RelatesToType;

import java.util.List;
import java.util.Optional;

/**
 * Mapper to map between WS-Addressing JAXB elements and {@link WsAddressingHeader}.
 */
public class WsAddressingMapper {
    private final JaxbUtil jaxbUtil;
    private final ObjectFactory wsaFactory;
    private final WsAddressingUtil wsaUtil;

    @Inject
    WsAddressingMapper(JaxbUtil jaxbUtil,
                       ObjectFactory wsaFactory,
                       WsAddressingUtil wsaUtil) {
        this.jaxbUtil = jaxbUtil;
        this.wsaFactory = wsaFactory;
        this.wsaUtil = wsaUtil;
    }

    /**
     * The mapper function that takes a convenience WS-Addressing header and adds it to a list of JAXB objects.
     *
     * @param src  The WS-Addressing source information.
     * @param dest The list of objects where to add the mapped JAXB element (typically the list of headers).
     */
    public void mapToJaxbSoapHeader(WsAddressingHeader src, List<Object> dest) {
        src.getAction().ifPresent(attributedURIType -> dest.add(wsaFactory.createAction(attributedURIType)));
        src.getMessageId().ifPresent(attributedURIType -> dest.add(wsaFactory.createMessageID(attributedURIType)));
        src.getTo().ifPresent(attributedURIType -> dest.add(wsaFactory.createTo(attributedURIType)));
        src.getRelatesTo().ifPresent(attributedURIType -> {
            RelatesToType relatesToType = wsaFactory.createRelatesToType();
            relatesToType.setValue(attributedURIType.getValue());
            dest.add(wsaFactory.createRelatesTo(relatesToType));
        });
    }

    /**
     * The mapper function that takes a list of JAXB objects and populates the convenience WS-Addressing header.
     *
     * @param src  The list of objects where to get the WS-Addressing header information from.
     * @param dest The WS-Addressing mapper destination.
     */
    public void mapFromJaxbSoapHeader(List<Object> src, WsAddressingHeader dest) {
        src.forEach(jaxbObject -> {
            Optional<AttributedURIType> uri;

            uri = jaxbUtil.extractElement(jaxbObject, WsAddressingConstants.ACTION);
            if (uri.isPresent() && dest.getAction().isEmpty()) {
                dest.setAction(uri.get());
            }

            uri = jaxbUtil.extractElement(jaxbObject, WsAddressingConstants.MESSAGE_ID);
            if (uri.isPresent() && dest.getMessageId().isEmpty()) {
                dest.setMessageId(uri.get());
            }

            uri = jaxbUtil.extractElement(jaxbObject, WsAddressingConstants.TO);
            if (uri.isPresent() && dest.getTo().isEmpty()) {
                dest.setTo(uri.get());
            }

            Optional<RelatesToType> rt = jaxbUtil.extractElement(jaxbObject, WsAddressingConstants.RELATES_TO);
            if (rt.isPresent() && dest.getRelatesTo().isEmpty()) {
                dest.setRelatesTo(wsaUtil.createAttributedURIType(rt.get().getValue()));
            }
        });
    }
}
