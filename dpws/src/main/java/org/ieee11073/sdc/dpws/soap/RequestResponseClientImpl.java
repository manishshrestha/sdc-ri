package org.ieee11073.sdc.dpws.soap;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.ieee11073.sdc.dpws.soap.exception.MarshallingException;
import org.ieee11073.sdc.dpws.soap.exception.SoapFaultException;
import org.ieee11073.sdc.dpws.soap.exception.TransportException;
import org.ieee11073.sdc.dpws.soap.interception.*;
import org.ieee11073.sdc.dpws.soap.wsaddressing.WsAddressingClientInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@linkplain RequestResponseClient}.
 */
public class RequestResponseClientImpl implements RequestResponseClient {
    private static final Logger LOG = LoggerFactory.getLogger(RequestResponseClientImpl.class);

    private final InterceptorRegistry interceptorRegistry;
    private final RequestResponseCallback networkCallback;
    private final ClientHelper clientHelper;

    @AssistedInject
    RequestResponseClientImpl(@Assisted RequestResponseCallback networkCallback,
                              ClientHelper clientHelper,
                              InterceptorRegistry interceptorRegistry,
                              WsAddressingClientInterceptor wsaClientInterceptor) {
        this.networkCallback = networkCallback;
        this.clientHelper = clientHelper;
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
            throws SoapFaultException, MarshallingException, TransportException {
        RequestObject rObj = new RequestObject(request);
        clientHelper.invokeDispatcher(Direction.REQUEST, interceptorRegistry, request, rObj);

        SoapMessage response = networkCallback.onRequestResponse(request);
        if (response.isFault()) {
            throw new SoapFaultException(response);
        }

        RequestResponseObject rrObj = new RequestResponseObject(request, response);
        clientHelper.invokeDispatcher(Direction.RESPONSE, interceptorRegistry, response, rrObj);

        return response;
    }
}
