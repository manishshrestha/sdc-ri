package org.ieee11073.sdc.dpws.soap.interception;

import org.ieee11073.sdc.dpws.soap.exception.SoapFaultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Optional;

/**
 * Runs interceptors.
 * <p>
 * todo DGr throw InterceptorException that wraps other exception in order to trace exception source
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
     * @return interceptor result accumulated from all invoked interceptors.
     * @throws SoapFaultException if a SOAP fault comes up.
     */
    InterceptorResult dispatch(Direction direction,
                               InterceptorRegistry interceptorRegistry,
                               @Nullable String action,
                               InterceptorCallbackType callbackData) throws InterceptorException {
        // First apply default interceptors
        InterceptorResult interceptorResult = invokeInterceptors(direction, callbackData,
                interceptorRegistry.getDefaultInterceptors());
        if (interceptorResult == InterceptorResult.CANCEL) {
            return InterceptorResult.CANCEL;
        }

        // Second apply specific interceptors
        if (Optional.ofNullable(action).isPresent()) {
            Collection<InterceptorInfo> interceptors = interceptorRegistry.getInterceptors(action);
            return invokeInterceptors(direction, callbackData, interceptors);
        } else {
            return InterceptorResult.NONE_INVOKED;
        }
    }

    private InterceptorResult invokeInterceptors(Direction direction,
                                                 InterceptorCallbackType callbackParam,
                                                 Collection<InterceptorInfo> interceptors) throws InterceptorException {
        InterceptorResult interceptorResult = InterceptorResult.NONE_INVOKED;
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

                if (!callbackMethod.getReturnType().equals(Void.TYPE)) {
                    InterceptorResult iResult = (InterceptorResult) callbackMethod.invoke(
                            interceptorInfo.getCallbackObject(), callbackParam);
                    if (iResult == InterceptorResult.CANCEL) {
                        return InterceptorResult.CANCEL;
                    }
                } else {
                    callbackMethod.invoke(interceptorInfo.getCallbackObject(), callbackParam);
                }
            } catch (IllegalAccessException e) {
                LOG.warn(e.getMessage());
            } catch (InvocationTargetException e) {
                throw new InterceptorException("Exception thrown by interceptor " +
                        interceptorInfo.getCallbackObject().toString(),
                        interceptorInfo.getCallbackObject(), e.getTargetException());
            }
            interceptorResult = InterceptorResult.PROCEED;
        }

        return interceptorResult;
    }
}
