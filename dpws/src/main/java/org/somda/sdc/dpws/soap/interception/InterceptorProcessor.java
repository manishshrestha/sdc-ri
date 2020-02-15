package org.somda.sdc.dpws.soap.interception;

import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;

/**
 * Runs interceptors.
 */
class InterceptorProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(InterceptorProcessor.class);

    /**
     * Dispatches callback data to all interceptor methods from the given registry.
     *
     * @param direction           the interceptor direction to capture.
     * @param interceptorRegistry the registry where to seek actions.
     * @param action              the affected action.
     * @param callbackData        the data to dispatch to found interceptors.
     * @throws SoapFaultException if a SOAP fault comes up.
     */
    void dispatch(Direction direction,
                               InterceptorRegistry interceptorRegistry,
                               @Nullable String action,
                               InterceptorCallbackType callbackData) throws InterceptorException {
        // First apply default interceptors
        invokeInterceptors(direction, callbackData, interceptorRegistry.getDefaultInterceptors());

        // Second apply specific interceptors
        Collection<InterceptorInfo> interceptors = interceptorRegistry.getInterceptors(action);
        invokeInterceptors(direction, callbackData, interceptors);
    }

    private void invokeInterceptors(Direction direction,
                                    InterceptorCallbackType callbackParam,
                                    Collection<InterceptorInfo> interceptors) throws InterceptorException {
        for (InterceptorInfo interceptorInfo : interceptors) {
            Method callbackMethod = interceptorInfo.getCallbackMethod();
            try {
                if (callbackMethod.getParameterCount() != 1 ||
                        !callbackMethod.getParameterTypes()[0].isAssignableFrom(callbackParam.getClass())) {
                    continue;
                }

                Direction directionFromAnnotation = callbackMethod.getDeclaredAnnotation(
                        MessageInterceptor.class).direction();
                if (directionFromAnnotation != Direction.ANY) {
                    if (direction != directionFromAnnotation) {
                        continue;
                    }
                }

                callbackMethod.invoke(interceptorInfo.getCallbackObject(), callbackParam);
            } catch (IllegalAccessException e) {
                LOG.warn(e.getMessage());
                LOG.trace("Error while calling interceptor", e);
            } catch (InvocationTargetException e) {
                LOG.trace("Error while calling interceptor", e);
                throw new InterceptorException("Exception thrown by interceptor " +
                        interceptorInfo.getCallbackObject().toString(),
                        interceptorInfo.getCallbackObject(), e.getTargetException());
            }
        }
    }
}
