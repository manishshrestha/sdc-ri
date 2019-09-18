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
 */
public class ClientHelper {
    private static final Logger LOG = LoggerFactory.getLogger(ClientHelper.class);
    private final InterceptorInvoker interceptorInvoker;

    @Inject
    ClientHelper(InterceptorInvoker interceptorInvoker) {
        this.interceptorInvoker = interceptorInvoker;
    }

    /**
     * Dispatch *param* to interceptors from *registry* matching given action in *msg* and *direction*.
     *
     * In contrast to {@link ServerHelper}, which throws  {@link SoapFaultException} on
     * {@link InterceptorResult#CANCEL}, {@linkplain ClientHelper} throws an {@link InterceptorException} on
     * {@link InterceptorResult#CANCEL} and {@link InterceptorResult#SKIP_RESPONSE}.
     *
     * @param direction direction to match
     * @param registry registry to retrieve interceptors from
     * @param msg message to match action from
     * @param param parameter to dispatch
     * @return result of dispatch
     */
    public InterceptorResult invokeDispatcher(Direction direction,
                                              InterceptorRegistry registry,
                                              SoapMessage msg,
                                              InterceptorCallbackType param) {
        Optional<AttributedURIType> action = msg.getWsAddressingHeader().getAction();
        String actionUri = null;
        if (action.isPresent()) {
            actionUri = action.get().getValue();
        }

        try {
            return interceptorInvoker.dispatch(direction, registry, actionUri, param);
        } catch (Exception e) {
            LOG.warn("Exception thrown during dispatcher invocation routine: {}", e.getMessage());
            throw new RuntimeException("An interceptor quit with an exception", e);
        }
    }
}
