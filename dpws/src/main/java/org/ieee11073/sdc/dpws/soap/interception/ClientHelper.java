package org.ieee11073.sdc.dpws.soap.interception;

import com.google.inject.Inject;
import org.ieee11073.sdc.dpws.soap.SoapMessage;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.AttributedURIType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Interceptor dispatcher designed for outgoing calls on clients.
 * <p>
 * todo DGr rename class to ClientDispatcher
 */
public class ClientHelper {
    private static final Logger LOG = LoggerFactory.getLogger(ClientHelper.class);
    private final InterceptorProcessor interceptorProcessor;

    @Inject
    ClientHelper(InterceptorProcessor interceptorProcessor) {
        this.interceptorProcessor = interceptorProcessor;
    }

    /**
     * Starts dispatching a SOAP message along an interceptor chain.
     *
     * @param direction                 the communication direction used for dispatching.
     * @param registry                  the interceptor registry used to seek interceptors.
     * @param soapMessage               the SOAP message to dispatch.
     * @param interceptorCallbackObject the object where to dispatch the message to.
     * @return the interceptor result.
     * @throws InterceptorException in case any exception is thrown.
     */
    public InterceptorResult invokeDispatcher(Direction direction,
                                              InterceptorRegistry registry,
                                              SoapMessage soapMessage,
                                              InterceptorCallbackType interceptorCallbackObject) throws InterceptorException {
        Optional<AttributedURIType> action = soapMessage.getWsAddressingHeader().getAction();
        String actionUri = null;
        if (action.isPresent()) {
            actionUri = action.get().getValue();
        }

        return interceptorProcessor.dispatch(direction, registry, actionUri, interceptorCallbackObject);
    }
}
