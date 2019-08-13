package org.ieee11073.sdc.biceps.common;

public class DuplicateHandleException extends RuntimeException {
    public DuplicateHandleException() {
    }

    public DuplicateHandleException(String message) {
        super(message);
    }

    public DuplicateHandleException(String message, Throwable cause) {
        super(message, cause);
    }

    public DuplicateHandleException(Throwable cause) {
        super(cause);
    }

    public DuplicateHandleException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
