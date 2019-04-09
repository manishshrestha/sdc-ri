package org.ieee11073.sdc.dpws.soap.interception;

/**
 * Interface to registerOrUpdate interceptor object for message interception chains.
 *
 * @see MessageInterceptor
 */
public interface InterceptorHandler {
    /**
     * Register callback object.
     *
     * Use {@link MessageInterceptor} annotation to define interceptor callbacks.
     *
     * @param interceptor Any objects that possess zero or more {@link MessageInterceptor} annotations.
     */
    void register(Interceptor interceptor);
}
