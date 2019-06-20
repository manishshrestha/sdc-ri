package org.ieee11073.sdc.dpws;

/**
 * Exception that occurs during processing within a transport binding.
 */
public class TransportBindingException extends RuntimeException {
    public TransportBindingException() {
    }

    public TransportBindingException(String message) {
        super(message);
    }

    public TransportBindingException(String message, Throwable cause) {
        super(message, cause);
    }

    public TransportBindingException(Throwable cause) {
        super(cause);
    }

    public TransportBindingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
