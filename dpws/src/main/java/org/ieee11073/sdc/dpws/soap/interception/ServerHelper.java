package org.ieee11073.sdc.dpws.soap.interception;

import com.google.inject.Inject;
import org.ieee11073.sdc.dpws.soap.SoapMessage;
import org.ieee11073.sdc.dpws.soap.exception.SoapFaultException;
import org.ieee11073.sdc.dpws.soap.factory.SoapFaultFactory;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.AttributedURIType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Interceptor dispatcher designed for incoming messages on servers.
 */
public class ServerHelper {
    private static final Logger LOG = LoggerFactory.getLogger(ServerHelper.class);

    private final InterceptorInvoker interceptorInvoker;
    private final SoapFaultFactory soapFaultFactory;

    @Inject
    public ServerHelper(InterceptorInvoker interceptorInvoker,
                        SoapFaultFactory soapFaultFactory) {
        this.interceptorInvoker = interceptorInvoker;
        this.soapFaultFactory = soapFaultFactory;
    }

    /**
     * Start dispatching a SOAP message along an interceptor chain.
     * <p>
     * In contrast to {@link ClientHelper}, which throws {@link InterceptorException} on
     * {@link InterceptorResult#CANCEL} and {@link InterceptorResult#SKIP_RESPONSE}, {@linkplain ServerHelper} throws a
     * {@link SoapFaultException} on {@link InterceptorResult#CANCEL}.
     *
     * @param direction the communication direction used for dispatching.
     * @param registry the interceptor registry used to seek interceptors.
     * @param soapMessage the SOAP message to dispatch.
     * @param interceptorCallbackObject the object where to dispatch the message to.
     * @return the interceptor result.
     * @throws SoapFaultException if the interceptor was cancelled (in order to communicate this an error to the client).
     */
    public InterceptorResult invokeDispatcher(Direction direction,
                                              InterceptorRegistry registry,
                                              SoapMessage soapMessage,
                                              InterceptorCallbackType interceptorCallbackObject) throws SoapFaultException {
        Optional<AttributedURIType> action = soapMessage.getWsAddressingHeader().getAction();
        String actionUri = null;
        if (action.isPresent()) {
            actionUri = action.get().getValue();
        }

        try {
            return interceptorInvoker.dispatch(direction, registry, actionUri, interceptorCallbackObject);
        } catch (SoapFaultException e) {
            throw e;
        } catch (Exception e) {
            LOG.warn("Unknown exception thrown during dispatcher invocation routine: {}", e.getMessage());
            throw new SoapFaultException(soapFaultFactory.createReceiverFault(
                    String.format("Server fault information: %s", e.getMessage())));
        }
    }
}
