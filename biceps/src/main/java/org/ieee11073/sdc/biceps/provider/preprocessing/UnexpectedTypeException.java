package org.ieee11073.sdc.biceps.provider.preprocessing;

public class UnexpectedTypeException extends Exception{
    public UnexpectedTypeException() {
    }

    public UnexpectedTypeException(String message) {
        super(message);
    }

    public UnexpectedTypeException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnexpectedTypeException(Throwable cause) {
        super(cause);
    }

    public UnexpectedTypeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
