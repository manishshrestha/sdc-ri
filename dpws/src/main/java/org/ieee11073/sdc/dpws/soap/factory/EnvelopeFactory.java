package org.ieee11073.sdc.dpws.soap.factory;

import com.google.inject.Inject;
import org.ieee11073.sdc.dpws.soap.model.Body;
import org.ieee11073.sdc.dpws.soap.model.Envelope;
import org.ieee11073.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.AttributedURIType;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.ObjectFactory;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Convenience factory to create SOAP envelopes in accordance to JAXB SOAP model.
 */
public class EnvelopeFactory {

    private final ObjectFactory wsaFactory;
    private final org.ieee11073.sdc.dpws.soap.model.ObjectFactory soapFactory;
    private final WsAddressingUtil wsaUtil;

    @Inject
    EnvelopeFactory(ObjectFactory wsaFactory,
                    org.ieee11073.sdc.dpws.soap.model.ObjectFactory soapFactory,
                    WsAddressingUtil wsaUtil) {
        this.wsaFactory = wsaFactory;
        this.soapFactory = soapFactory;
        this.wsaUtil = wsaUtil;

    }

    /**
     * Create envelope with given wsa:Action attribute and body.
     *
     * @param wsaAction Action to use for Envelope
     * @param firstBodyChild message body
     * @return an {@link Envelope}
     */
    public Envelope createEnvelope(String wsaAction, @Nullable Object firstBodyChild) {
        Envelope envelope = createEnvelope(firstBodyChild);
        envelope.getHeader().getAny().add(wsaFactory.createAction(createUri(wsaAction)));
        return envelope;
    }

    /**
     * Create envelope with given wsa:Action attribute, wsa:To attribute, and body.
     *
     * @param wsaAction Action to use for Envelope
     * @param firstBodyChild message body
     * @param wsaTo wsa:To element content
     * @return an {@link Envelope}
     */
    public Envelope createEnvelope(String wsaAction, String wsaTo, @Nullable Object firstBodyChild) {
        Envelope envelope = createEnvelope(firstBodyChild);
        envelope.getHeader().getAny().add(wsaFactory.createAction(createUri(wsaAction)));
        envelope.getHeader().getAny().add(wsaFactory.createTo(createUri(wsaTo)));
        return envelope;
    }

    /**
     * Create envelope with given JAXB body element.
     *
     * Any header fields will be left empty.
     *
     * @param firstBodyChild element to store in body
     * @return an {@link Envelope}
     */
    public Envelope createEnvelope(@Nullable Object firstBodyChild) {
        Envelope envelope = soapFactory.createEnvelope();
        envelope.setHeader(soapFactory.createHeader());
        envelope.setBody(soapFactory.createBody());
        Optional.ofNullable(firstBodyChild).ifPresent(jaxbElement -> envelope.getBody().getAny().add(jaxbElement));
        return envelope;
    }

    /**
     * Create an empty SOAP envelope with existing empty body and header.
     * @return an empty {@link Envelope}
     */
    public Envelope createEnvelope() {
        return createEnvelope(null);
    }

    public Envelope createEnvelopeFromBody(Body body) {
        Envelope envelope = createEnvelope(null);
        envelope.setBody(body);
        return envelope;
    }

    private AttributedURIType createUri(String uri) {
        return wsaUtil.createAttributedURIType(uri);
    }
}
