package org.somda.sdc.dpws.soap;

import com.google.inject.Inject;
import org.somda.sdc.common.util.JaxbUtil;
import org.somda.sdc.common.util.UriUtil;
import org.somda.sdc.dpws.soap.factory.EnvelopeFactory;
import org.somda.sdc.dpws.soap.factory.SoapMessageFactory;
import org.somda.sdc.dpws.soap.model.Envelope;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;

import javax.annotation.Nullable;
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
     * Stores element in destined SOAP body.
     *
     * @param <T>     the type of element in {@code element}.
     * @param element element to store in {@code dest}.
     * @param dest    SOAP message to store {@code element} in.
     */
    public <T> void setBody(T element, SoapMessage dest) {
        dest.getOriginalEnvelope().getBody().getAny().clear();
        dest.getOriginalEnvelope().getBody().getAny().add(element);
    }

    /**
     * Tries to retrieve SOAP body element from {@link SoapMessage}.
     *
     * @param src      the source message.
     * @param <T>      the type to convert to.
     * @param destType type of first element in body.
     * @return an instance of T, or {@linkplain Optional#empty} if conversion fails.
     */
    public <T> Optional<T> getBody(SoapMessage src, Class<T> destType) {
        return jaxbUtil.extractFirstElementFromAny(src.getOriginalEnvelope().getBody().getAny(), destType);
    }

    /**
     * Creates a URI representation of a UUID.
     *
     * @param uuid the UUID to convert.
     * @return a UUID URN.
     */
    public String createUriFromUuid(UUID uuid) {
        return UriUtil.createUuid(uuid);
    }

    /**
     * Converts from a URI to a UUID.
     *
     * @param uri the URI to convert.
     * @return the converted UUID.
     * @throws RuntimeException in case of misformatted URIs.
     */
    public UUID createUuidFromUri(String uri) {
        return UUID.fromString(uri.substring("urn:uuid:".length()));
    }

    /**
     * Creates a URI with a random UUID.
     *
     * @return the UUID URN.
     */
    public String createRandomUuidUri() {
        return createUriFromUuid(UUID.randomUUID());
    }

    /**
     * Takes a response message and sets the WS-Addressing action header.
     *
     * @param response the SOAP response message.
     * @param action   the action to put to the response message.
     */
    public void setWsaAction(SoapMessage response, String action) {
        response.getWsAddressingHeader().setAction(wsaUtil.createAttributedURIType(action));
    }

    /**
     * Creates an empty SOAP message.
     *
     * @return a new SOAP message.
     */
    public SoapMessage createMessage() {
        return soapMessageFactory.createSoapMessage(envelopeFactory.createEnvelope());
    }

    /**
     * Creates a SOAP message with the given envelope.
     *
     * @param envelope the envelope to put to the SOAP message.
     * @return a new SOAP message.
     */
    public SoapMessage createMessage(Envelope envelope) {
        return soapMessageFactory.createSoapMessage(envelope);
    }

    /**
     * Creates a SOAP message with a specific action and empty body.
     *
     * @param wsaAction the action header to set.
     * @return a new SOAP message.
     */
    public SoapMessage createMessage(String wsaAction) {
        var message = soapMessageFactory.createSoapMessage(envelopeFactory.createEnvelope());
        message.getWsAddressingHeader().setAction(wsaUtil.createAttributedURIType(wsaAction));
        return message;
    }

    /**
     * Creates a SOAP message with a specific action and first body element.
     *
     * @param wsaAction        the action header to set.
     * @param firstBodyElement the first child of the body element.
     * @return a new SOAP message.
     */
    public SoapMessage createMessage(String wsaAction, @Nullable Object firstBodyElement) {
        var message = soapMessageFactory.createSoapMessage(envelopeFactory.createEnvelope(
                firstBodyElement
        ));
        message.getWsAddressingHeader().setAction(wsaUtil.createAttributedURIType(wsaAction));
        return message;
    }

    /**
     * Creates a SOAP message with a specific action and to element.
     *
     * @param wsaAction the action header to set.
     * @param wsaTo     the to header to set.
     * @return a new SOAP message.
     */
    public SoapMessage createMessage(String wsaAction, String wsaTo) {
        var message = soapMessageFactory.createSoapMessage(envelopeFactory.createEnvelope());
        message.getWsAddressingHeader().setAction(wsaUtil.createAttributedURIType(wsaAction));
        message.getWsAddressingHeader().setTo(wsaUtil.createAttributedURIType(wsaTo));
        return message;
    }

    /**
     * Creates a SOAP message with a specific action, to and first body element.
     *
     * @param wsaAction        the action header to set.
     * @param wsaTo            the to header to set.
     * @param firstBodyElement the first child of the body element.
     * @return a new SOAP message.
     */
    public SoapMessage createMessage(String wsaAction, String wsaTo, @Nullable Object firstBodyElement) {
        var message = soapMessageFactory.createSoapMessage(envelopeFactory.createEnvelope(firstBodyElement));
        message.getWsAddressingHeader().setAction(wsaUtil.createAttributedURIType(wsaAction));
        message.getWsAddressingHeader().setTo(wsaUtil.createAttributedURIType(wsaTo));
        return message;
    }
}
