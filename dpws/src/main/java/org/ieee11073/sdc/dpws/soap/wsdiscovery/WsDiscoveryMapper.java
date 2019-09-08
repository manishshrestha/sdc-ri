package org.ieee11073.sdc.dpws.soap.wsdiscovery;

import com.google.inject.Inject;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.model.AppSequenceType;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.model.ObjectFactory;
import org.ieee11073.sdc.common.helper.JaxbUtil;

import java.util.List;
import java.util.Optional;

/**
 * Mapper to map between WS-Discovery JAXB elements and {@link WsDiscoveryHeader}.
 */
public class WsDiscoveryMapper {
    private final JaxbUtil jaxbUtil;
    private final ObjectFactory wsdFactory;

    @Inject
    WsDiscoveryMapper(JaxbUtil jaxbUtil, ObjectFactory wsdFactory) {
        this.jaxbUtil = jaxbUtil;
        this.wsdFactory = wsdFactory;
    }

    public void mapToJaxbSoapHeader(WsDiscoveryHeader src, List<Object> dest) {
        src.getAppSequence().ifPresent(appSequenceType -> dest.add(wsdFactory.createAppSequence(appSequenceType)));
    }

    public void mapFromJaxbSoapHeader(List<Object> src, WsDiscoveryHeader dest) {
        src.forEach(o -> {
            Optional<AppSequenceType> appSeq;
            appSeq = jaxbUtil.extractElement(o, WsDiscoveryConstants.APP_SEQUENCE);
            if (appSeq.isPresent() && dest.getAppSequence().isEmpty()) {
                dest.setAppSequence(appSeq.get());
            }
        });
    }
}
