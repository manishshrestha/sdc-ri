package org.ieee11073.sdc.dpws.soap;

import com.google.inject.Inject;
import org.ieee11073.sdc.dpws.soap.factory.EnvelopeFactory;
import org.ieee11073.sdc.dpws.soap.factory.SoapMessageFactory;
import org.ieee11073.sdc.dpws.soap.model.Envelope;
import org.ieee11073.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.ieee11073.sdc.common.helper.JaxbUtil;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

/**
 * SOAP utility functions.
 */
public class SoapUtil {
    private final WsAddressingUtil wsaUtil;
    private final SoapMessageFactory soapMessageFactory;
    private final EnvelopeFactory envelopeFactory;
    private final JaxbUtil jaxbUtil;

    @Inject
    SoapUtil(WsAddressingUtil wsaUtil,
             SoapMessageFactory soapMessageFactory,
             EnvelopeFactory envelopeFactory,
             JaxbUtil jaxbUtil) {
        this.wsaUtil = wsaUtil;
        this.soapMessageFactory = soapMessageFactory;
        this.envelopeFactory = envelopeFactory;
        this.jaxbUtil = jaxbUtil;
    }

    /**
     * Store element in destined SOAP body.
     */
    public <T> void setBody(T element, SoapMessage dest) {
        dest.getOriginalEnvelope().getBody().getAny().clear();
        dest.getOriginalEnvelope().getBody().getAny().add(element);
    }

    /**
     * Try to retrieve SOAP body element from {@link SoapMessage}.
     *
     * @param src The source message.
     * @param <T> The type to convert to.
     * @return An instance of T, or {@linkplain Optional#empty} if conversion fails.
     */
    public <T> Optional<T> getBody(SoapMessage src, Class<T> destType) {
        return jaxbUtil.extractFirstElementFromAny(src.getOriginalEnvelope().getBody().getAny(), destType);
    }

    public URI createUriFromUuid(UUID uuid) {
        return URI.create("urn:uuid:" + uuid.toString());
    }

    public UUID createUuidFromUri(URI uri) {
        return UUID.fromString(uri.toString().substring("urn:uuid:".length()));
    }

    public URI createRandomUuidUri() {
        return createUriFromUuid(UUID.randomUUID());
    }

    public void setWsaAction(SoapMessage response, String action) {
        response.getWsAddressingHeader().setAction(wsaUtil.createAttributedURIType(action));
    }

    public SoapMessage createMessage() {
        return soapMessageFactory.createSoapMessage(envelopeFactory.createEnvelope());
    }

    public SoapMessage createMessage(Envelope envelope) {
        return soapMessageFactory.createSoapMessage(envelope);
    }

    public SoapMessage createMessage(String wsaAction) {
        return soapMessageFactory.createSoapMessage(envelopeFactory.createEnvelope(wsaAction, null));
    }

    public SoapMessage createMessage(String wsaAction, @Nullable Object firstBodyElement) {
        return soapMessageFactory.createSoapMessage(envelopeFactory.createEnvelope(wsaAction, firstBodyElement));
    }

    public SoapMessage createMessage(String wsaAction, String wsaTo) {
        return soapMessageFactory.createSoapMessage(envelopeFactory.createEnvelope(wsaAction, wsaTo, null));
    }

    public SoapMessage createMessage(String wsaAction, String wsaTo, @Nullable Object firstBodyElement) {
        return soapMessageFactory.createSoapMessage(envelopeFactory.createEnvelope(wsaAction, wsaTo, firstBodyElement));
    }
}
