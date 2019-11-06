package org.ieee11073.sdc.dpws.soap;


import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.ieee11073.sdc.dpws.soap.factory.EnvelopeFactory;
import org.ieee11073.sdc.dpws.soap.model.Envelope;
import org.ieee11073.sdc.dpws.soap.wsaddressing.WsAddressingHeader;
import org.ieee11073.sdc.dpws.soap.wsaddressing.WsAddressingMapper;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.WsDiscoveryHeader;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.WsDiscoveryMapper;
import org.ieee11073.sdc.dpws.soap.wseventing.helper.WsEventingHeader;
import org.ieee11073.sdc.dpws.soap.wseventing.helper.WsEventingMapper;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelling of a SOAP Message with convenient access to different headers.
 */
public class SoapMessage {
    private final WsDiscoveryHeader wsdHeader;
    private final WsAddressingHeader wsaHeader;
    private final WsEventingHeader wseHeader;
    private final Envelope envelope;
    private final WsAddressingMapper wsaMapper;
    private final WsDiscoveryMapper wsdMapper;
    private final EnvelopeFactory envelopeFactory;
    private final WsEventingMapper wseMapper;

    @AssistedInject
    SoapMessage(@Assisted Envelope envelope,
                Provider<WsAddressingHeader> wsaHeaderProvider,
                WsAddressingMapper wsaMapper,
                Provider<WsDiscoveryHeader> wsdHeaderProvider,
                WsDiscoveryMapper wsdMapper,
                Provider<WsEventingHeader> wseHeaderProvider,
                WsEventingMapper wseMapper,
                EnvelopeFactory envelopeFactory) {
        this.envelope = envelope;
        this.wsaMapper = wsaMapper;
        this.wsdMapper = wsdMapper;
        this.wseMapper = wseMapper;
        this.envelopeFactory = envelopeFactory;

        List<Object> tmpHeaderList = new ArrayList<>();
        if (envelope.getHeader() != null && envelope.getHeader().getAny() != null) {
            tmpHeaderList = envelope.getHeader().getAny();
        }

        wsaHeader = wsaHeaderProvider.get();
        wsaMapper.mapFromJaxbSoapHeader(tmpHeaderList, wsaHeader);

        wsdHeader = wsdHeaderProvider.get();
        wsdMapper.mapFromJaxbSoapHeader(tmpHeaderList, wsdHeader);

        wseHeader = wseHeaderProvider.get();
        wseMapper.mapFromJaxbSoapHeader(tmpHeaderList, wseHeader);
    }

    public WsAddressingHeader getWsAddressingHeader() {
        return wsaHeader;
    }

    public WsDiscoveryHeader getWsDiscoveryHeader() {
        return wsdHeader;
    }

    public WsEventingHeader getWsEventingHeader() {
        return wseHeader;
    }

    /**
     * Gets the original envelope.
     *
     * @return always returns the envelope as passed to the constructor.
     * Information that is stored in convenience headers are not synchronized with this envelope.
     */
    public Envelope getOriginalEnvelope() {
        return envelope;
    }

    /**
     * Gets the envelope that includes mapped headers.
     *
     * @return new envelope with mapped convenience headers and body reference from {@link #getOriginalEnvelope()}.
     */
    public Envelope getEnvelopeWithMappedHeaders() {
        Envelope mappedEnv = envelopeFactory.createEnvelopeFromBody(envelope.getBody());
        List<Object> tmpHeaderList = mappedEnv.getHeader().getAny();

        wsaMapper.mapToJaxbSoapHeader(wsaHeader, tmpHeaderList);
        wsdMapper.mapToJaxbSoapHeader(wsdHeader, tmpHeaderList);
        wseMapper.mapToJaxbSoapHeader(wseHeader, tmpHeaderList);

        return mappedEnv;
    }

    /**
     * Checks if a SOAP message is a fault or not.
     *
     * @return true if it is a fault, false otherwise.
     */
    public boolean isFault() {
        if (getOriginalEnvelope().getBody().getAny().size() == 1) {
            Object obj = getOriginalEnvelope().getBody().getAny().get(0);
            if (JAXBElement.class.isAssignableFrom(obj.getClass())) {
                JAXBElement jaxbElem = (JAXBElement) obj;
                return jaxbElem.getName().equals(SoapConstants.FAULT);
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return SoapDebug.getBrief(this);
    }
}
