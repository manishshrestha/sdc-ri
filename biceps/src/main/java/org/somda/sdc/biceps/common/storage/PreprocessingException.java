package org.somda.sdc.biceps.common.storage;

/**
 * An exception that is thrown if a preprocessing error occurs.
 */
public class PreprocessingException extends Exception {
    private final String segment;

    public PreprocessingException(String message, Throwable cause, String segment) {
        super(message, cause);
        this.segment = segment;
    }

    public PreprocessingException(String message, String segment) {
        super(message);
        this.segment = segment;
    }

    public String getSegment() {
        return segment;
    }
}
