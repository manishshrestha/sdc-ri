package org.somda.sdc.mdpws.consumer.exception;

/**
 * An exception that can be raised during XPath parsing.
 */
public class XPathParseException extends Exception {
    public XPathParseException() {
    }

    public XPathParseException(String message) {
        super(message);
    }

    public XPathParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public XPathParseException(Throwable cause) {
        super(cause);
    }

    public XPathParseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
