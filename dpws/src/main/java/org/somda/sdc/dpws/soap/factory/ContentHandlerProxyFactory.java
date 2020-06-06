package org.somda.sdc.dpws.soap.factory;

import org.xml.sax.ContentHandler;

/**
 * An interface to be implemented to allow SAX XML parser hooks during JAXB unmarshalling.
 * <p>
 * This proxy factory allows to intercept JAXB unmarshalling performed by
 * {@link org.somda.sdc.dpws.helper.JaxbMarshalling}, e.g. to read XML namespaces or modify input data towards the
 * JAXB content handler.
 * <p>
 * <em>Important note: only bind this factory with Guice if you are going to do some actual work other than just
 * forwarding data. {@link org.somda.sdc.dpws.soap.JaxbSoapMarshalling} runs faster if there is no binding in
 * place.</em>
 */
public interface ContentHandlerProxyFactory {
    /**
     * Creates a content handler that serves as a proxy for the actual JAXB content handler.
     *
     * @param targetHandler the JAXB content handler that needs to be invoked by the created proxy in order to
     *                      correctly unmarshal XML input to JAXB objects.
     * @return a customized content handler.
     * @see org.somda.sdc.dpws.soap.ContentHandlerAdapter
     */
    ContentHandler createContentHandlerProxy(ContentHandler targetHandler);
}
