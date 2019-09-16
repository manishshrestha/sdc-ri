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
 * Dispatch callback parameter to interceptors.
 */
class InterceptorInvoker {
    private static final Logger LOG = LoggerFactory.getLogger(InterceptorInvoker.class);

    /**
     * Dispatch callbackParam to all interceptor methods in interceptorRegistry annotated with direction and action.
     *
     * @return Interceptor result accumulated from all invoked interceptors.
     */
    InterceptorResult dispatch(Direction direction,
                               InterceptorRegistry interceptorRegistry,
                               @Nullable String action,
                               InterceptorCallbackType callbackParam) throws SoapFaultException {
        InterceptorResult iResult = invokeInterceptors(direction, callbackParam,
                interceptorRegistry.getDefaultInterceptors());
        if (iResult == InterceptorResult.CANCEL) {
            return InterceptorResult.CANCEL;
        }

        if (Optional.ofNullable(action).isPresent()) {
            Collection<InterceptorInfo> interceptors = interceptorRegistry.getInterceptors(action);
            return invokeInterceptors(direction, callbackParam, interceptors);
        } else {
            return InterceptorResult.NONE_INVOKED;
        }
    }

    private InterceptorResult invokeInterceptors(Direction direction,
                                                 InterceptorCallbackType callbackParam,
                                                 Collection<InterceptorInfo> interceptors)
            throws SoapFaultException {

        InterceptorResult ir = InterceptorResult.NONE_INVOKED;
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
                if (e.getTargetException() instanceof SoapFaultException) {
                    throw (SoapFaultException) e.getTargetException();
                } else {
                    LOG.warn("Unexpected exception thrown", e.getTargetException());
                }
            }
            ir = InterceptorResult.PROCEED;
        }

        return ir;
    }
}
