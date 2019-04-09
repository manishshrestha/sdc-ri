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
     * Dispatch *param* to interceptors from *registry* matching given action in *msg* and *direction*.
     *
     * In contrast to {@link ClientHelper}, which throws {@link InterceptorException} on
     * {@link InterceptorResult#CANCEL} and {@link InterceptorResult#SKIP_RESPONSE}, {@linkplain ServerHelper} throws a
     * {@link SoapFaultException} on {@link InterceptorResult#CANCEL}.
     */
    public InterceptorResult invokeDispatcher(Direction direction,
                                              InterceptorRegistry registry,
                                              SoapMessage msg,
                                              InterceptorCallbackType param) throws SoapFaultException {
        Optional<AttributedURIType> action = msg.getWsAddressingHeader().getAction();
        String actionUri = null;
        if (action.isPresent()) {
            actionUri = action.get().getValue();
        }

        try {
            return interceptorInvoker.dispatch(direction, registry, actionUri, param);
        } catch (SoapFaultException e) {
            throw e;
        } catch (Exception e) {
            LOG.warn("Unknown exception thrown during dispatcher invocation routine: {}", e.getMessage());
            throw new SoapFaultException(soapFaultFactory.createReceiverFault(
                    String.format("Server fault information: %s", e.getMessage())));
        }
    }
}
