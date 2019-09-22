package org.ieee11073.sdc.biceps.common.preprocessing;

public class HandleDuplicatedException extends Exception {
    public HandleDuplicatedException() {
    }

    public HandleDuplicatedException(String message) {
        super(message);
    }

    public HandleDuplicatedException(String message, Throwable cause) {
        super(message, cause);
    }

    public HandleDuplicatedException(Throwable cause) {
        super(cause);
    }

    public HandleDuplicatedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
