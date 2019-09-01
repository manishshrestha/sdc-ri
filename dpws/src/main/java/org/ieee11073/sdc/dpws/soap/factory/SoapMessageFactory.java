package org.ieee11073.sdc.dpws.soap.factory;

import com.google.inject.assistedinject.Assisted;
import org.ieee11073.sdc.dpws.soap.SoapMessage;
import org.ieee11073.sdc.dpws.soap.model.Envelope;

/**
 * Guice factory interface to create {@link SoapMessage} instances.
 */
public interface SoapMessageFactory {
    /**
     * @param envelope The envelope that is encapsulated within the {@link SoapMessage}. All known headers will be
     *                 added to the convenience header access.
     * @return a new {@link SoapMessage} instance containing the {@code envelope}
     */
    SoapMessage createSoapMessage(@Assisted Envelope envelope);
}
