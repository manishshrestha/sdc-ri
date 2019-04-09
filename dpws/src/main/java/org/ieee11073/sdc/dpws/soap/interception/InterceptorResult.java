package org.ieee11073.sdc.dpws.soap.interception;

/**
 * Define progress of interceptor chain processing.
 */
public enum InterceptorResult {
    /**
     * Processing of interceptors shall be proceeded.
     */
    PROCEED,
    /**
     * Processing done, other interceptors shall be skipped, but result is ok.
     */
    SKIP_RESPONSE,
    /**
     * Only interceptor but default ones were invoked.
     */
    NONE_INVOKED,
    /**
     * Abnormal behaviour, cancel interceptor processing, no meaningful result can be provided.
     */
    CANCEL
}
