package org.somda.sdc.dpws.soap;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.interception.*;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingClientInterceptor;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Default implementation of {@linkplain RequestResponseClient}.
 */
public class RequestResponseClientImpl implements RequestResponseClient {
    private static final Logger LOG = LogManager.getLogger(RequestResponseClientImpl.class);

    private final InterceptorRegistry interceptorRegistry;
    private final RequestResponseCallback networkCallback;
    private final ClientDispatcher clientDispatcher;

    @AssistedInject
    RequestResponseClientImpl(@Assisted RequestResponseCallback networkCallback,
                              ClientDispatcher clientDispatcher,
                              InterceptorRegistry interceptorRegistry,
                              WsAddressingClientInterceptor wsaClientInterceptor) {
        this.networkCallback = networkCallback;
        this.clientDispatcher = clientDispatcher;
        this.interceptorRegistry = interceptorRegistry;

        // Enable WS-Addressing commons on this client
        register(wsaClientInterceptor);
    }

    @Override
    public void register(Interceptor interceptor) {
        interceptorRegistry.addInterceptor(interceptor);
    }

    @Override
    public SoapMessage sendRequestResponse(SoapMessage request)
            throws SoapFaultException, MarshallingException, TransportException, InterceptorException {
        RequestObject rObj = new RequestObject(request);
        clientDispatcher.invokeDispatcher(Direction.REQUEST, interceptorRegistry, request, rObj);

        SoapMessage response = networkCallback.onRequestResponse(request);
        if (response.isFault()) {
            throw new SoapFaultException(response);
        }

        RequestResponseObject rrObj = new RequestResponseObject(request, response);
        clientDispatcher.invokeDispatcher(Direction.RESPONSE, interceptorRegistry, response, rrObj);

        return response;
    }
}
