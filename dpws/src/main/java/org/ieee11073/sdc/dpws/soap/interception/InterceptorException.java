package org.ieee11073.sdc.dpws.soap.interception;

/**
 * Wrapped interceptor interceptorResult that is obtained in interceptor handling.
 */
public class InterceptorException extends RuntimeException {
    private InterceptorResult interceptorResult;
    public InterceptorException(InterceptorResult interceptorResult) {
        super(String.format("Interceptor interceptorResult: %s", interceptorResult.toString()));
    }

    public InterceptorException(String message, InterceptorResult interceptorResult) {
        super(message);
        this.interceptorResult = interceptorResult;
    }

    public InterceptorResult getInterceptorResult() {
        return interceptorResult;
    }
}
