package org.ieee11073.sdc.biceps.provider.preprocessing;

public class CardinalityException extends Exception {
    public CardinalityException() {
    }

    public CardinalityException(String message) {
        super(message);
    }

    public CardinalityException(String message, Throwable cause) {
        super(message, cause);
    }

    public CardinalityException(Throwable cause) {
        super(cause);
    }

    public CardinalityException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
