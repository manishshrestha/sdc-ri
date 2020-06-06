package org.somda.sdc.dpws.soap;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * Default implementation of a content handler that forwards XML events to a target handler.
 * <p>
 * This class can be used as an adapter for customized implementations to avoid unnecessary overrides.
 * It should not be used as a default binding as may cause a {@link org.somda.sdc.dpws.helper.JaxbMarshalling}
 * instance to perform code with no actual action other than forwarding data.
 */
public abstract class ContentHandlerAdapter implements ContentHandler {
    private final ContentHandler targetHandler;

    /**
     * @param targetHandler
     */
    public ContentHandlerAdapter(ContentHandler targetHandler) {
        this.targetHandler = targetHandler;
    }

    /**
     * Getter to retrieve the target handler that needs to be called to allow JAXB to unmarshal correctly.
     *
     * @return the target handler passed to the constructor.
     */
    protected ContentHandler getTargetHandler() {
        return targetHandler;
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        targetHandler.setDocumentLocator(locator);
    }

    @Override
    public void startDocument() throws SAXException {
        targetHandler.startDocument();
    }

    @Override
    public void endDocument() throws SAXException {
        targetHandler.endDocument();
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        targetHandler.startPrefixMapping(prefix, uri);
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
        targetHandler.endPrefixMapping(prefix);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        targetHandler.startElement(uri, localName, qName, atts);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        targetHandler.endElement(uri, localName, qName);
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        targetHandler.characters(ch, start, length);
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        targetHandler.ignorableWhitespace(ch, start, length);
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
        targetHandler.processingInstruction(target, data);
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
        targetHandler.skippedEntity(name);
    }
}
