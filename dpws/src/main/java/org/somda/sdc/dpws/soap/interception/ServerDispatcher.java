package org.somda.sdc.dpws.soap.interception;

import com.google.inject.Inject;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.factory.SoapFaultFactory;
import org.somda.sdc.dpws.soap.wsaddressing.model.AttributedURIType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Interceptor dispatcher designed for incoming messages on servers.
 */
public class ServerDispatcher {
    private static final Logger LOG = LoggerFactory.getLogger(ServerDispatcher.class);

    private final InterceptorProcessor interceptorProcessor;
    private final SoapFaultFactory soapFaultFactory;

    @Inject
    public ServerDispatcher(InterceptorProcessor interceptorProcessor,
                            SoapFaultFactory soapFaultFactory) {
        this.interceptorProcessor = interceptorProcessor;
        this.soapFaultFactory = soapFaultFactory;
    }

    /**
     * Starts dispatching a SOAP message along an interceptor chain.
     *
     * @param direction                 the communication direction used for dispatching.
     * @param registry                  the interceptor registry used to seek interceptors.
     * @param soapMessage               the SOAP message to dispatch.
     * @param interceptorCallbackObject the object where to dispatch the message to.
     * @throws SoapFaultException if the interceptor was cancelled (in order to communicate this as an error to the client).
     */
    public void invokeDispatcher(Direction direction,
                                              InterceptorRegistry registry,
                                              SoapMessage soapMessage,
                                              InterceptorCallbackType interceptorCallbackObject) throws SoapFaultException {
        Optional<AttributedURIType> action = soapMessage.getWsAddressingHeader().getAction();
        String actionUri = null;
        if (action.isPresent()) {
            actionUri = action.get().getValue();
        }

        try {
            interceptorProcessor.dispatch(direction, registry, actionUri, interceptorCallbackObject);
        } catch (InterceptorException e) {
            if (e.getCause() instanceof SoapFaultException) {
                throw (SoapFaultException) e.getCause();
            }
            throw new SoapFaultException(soapFaultFactory.createReceiverFault(
                    String.format("Server fault information: %s", e.getCause().getMessage())));
        } catch (Exception e) {
            LOG.warn("Unexpected exception thrown during dispatcher invocation routine: {}", e.getMessage());
            throw new SoapFaultException(soapFaultFactory.createReceiverFault(
                    String.format("Server fault information: %s", e.getMessage())));
        }
    }
}
