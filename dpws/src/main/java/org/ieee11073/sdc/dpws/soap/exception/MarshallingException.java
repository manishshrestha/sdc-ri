package org.ieee11073.sdc.dpws.soap.exception;


public class MarshallingException extends Exception {
    public MarshallingException() {
    }

    public MarshallingException(String message) {
        super(message);
    }

    public MarshallingException(String message, Throwable cause) {
        super(message, cause);
    }

    public MarshallingException(Throwable cause) {
        super(cause);
    }

    public MarshallingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
