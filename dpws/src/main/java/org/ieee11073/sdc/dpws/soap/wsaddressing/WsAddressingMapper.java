package org.ieee11073.sdc.dpws.soap.wsaddressing;

import com.google.inject.Inject;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.AttributedURIType;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.ObjectFactory;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.RelatesToType;
import org.ieee11073.sdc.common.helper.JaxbUtil;

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

    public void mapFromJaxbSoapHeader(List<Object> src, WsAddressingHeader dest) {
        src.forEach(o -> {
            Optional<AttributedURIType> uri;

            uri = jaxbUtil.extractElement(o, WsAddressingConstants.ACTION);
            if (uri.isPresent() && dest.getAction().isEmpty()) {
                dest.setAction(uri.get());
            }

            uri = jaxbUtil.extractElement(o, WsAddressingConstants.MESSAGE_ID);
            if (uri.isPresent() && dest.getMessageId().isEmpty()) {
                dest.setMessageId(uri.get());
            }

            uri = jaxbUtil.extractElement(o, WsAddressingConstants.TO);
            if (uri.isPresent() && dest.getTo().isEmpty()) {
                dest.setTo(uri.get());
            }

            Optional<RelatesToType> rt = jaxbUtil.extractElement(o, WsAddressingConstants.RELATES_TO);
            if (rt.isPresent() && dest.getRelatesTo().isEmpty()) {
                dest.setRelatesTo(wsaUtil.createAttributedURIType(rt.get().getValue()));
            }
        });
    }
}
