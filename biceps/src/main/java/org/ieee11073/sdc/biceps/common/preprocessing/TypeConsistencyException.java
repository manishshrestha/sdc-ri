package org.ieee11073.sdc.biceps.common.preprocessing;

public class TypeConsistencyException extends Exception{
    public TypeConsistencyException() {
    }

    public TypeConsistencyException(String message) {
        super(message);
    }

    public TypeConsistencyException(String message, Throwable cause) {
        super(message, cause);
    }

    public TypeConsistencyException(Throwable cause) {
        super(cause);
    }

    public TypeConsistencyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
