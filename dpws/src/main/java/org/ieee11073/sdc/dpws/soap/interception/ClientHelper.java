package org.ieee11073.sdc.dpws.soap.interception;

import com.google.inject.Inject;
import org.ieee11073.sdc.dpws.soap.SoapMessage;
import org.ieee11073.sdc.dpws.soap.exception.SoapFaultException;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.AttributedURIType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Interceptor dispatcher designed for outgoing calls on clients.
 *
 * todo DGr rename class to ClientDispatcher
 */
public class ClientHelper {
    private static final Logger LOG = LoggerFactory.getLogger(ClientHelper.class);
    private final InterceptorInvoker interceptorInvoker;

    @Inject
    ClientHelper(InterceptorInvoker interceptorInvoker) {
        this.interceptorInvoker = interceptorInvoker;
    }

    /**
     * Starts dispatching a SOAP message along an interceptor chain.
     *
     * @param direction the communication direction used for dispatching.
     * @param registry the interceptor registry used to seek interceptors.
     * @param soapMessage the SOAP message to dispatch.
     * @param interceptorCallbackObject the object where to dispatch the message to.
     * @return the interceptor result.
     * @throws RuntimeException in case any exception is thrown.
     */
    public InterceptorResult invokeDispatcher(Direction direction,
                                              InterceptorRegistry registry,
                                              SoapMessage soapMessage,
                                              InterceptorCallbackType interceptorCallbackObject) {
        Optional<AttributedURIType> action = soapMessage.getWsAddressingHeader().getAction();
        String actionUri = null;
        if (action.isPresent()) {
            actionUri = action.get().getValue();
        }

        try {
            return interceptorInvoker.dispatch(direction, registry, actionUri, interceptorCallbackObject);
        } catch (Exception e) {
            LOG.warn("Exception thrown during dispatcher invocation routine: {}", e.getMessage());
            throw new RuntimeException("An interceptor quit with an exception", e);
        }
    }
}
