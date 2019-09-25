package org.ieee11073.sdc.biceps.provider.preprocessing;

public class VersioningException extends Exception {
    public VersioningException() {
    }

    public VersioningException(String message) {
        super(message);
    }

    public VersioningException(String message, Throwable cause) {
        super(message, cause);
    }

    public VersioningException(Throwable cause) {
        super(cause);
    }

    public VersioningException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
