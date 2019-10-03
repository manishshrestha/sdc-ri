package org.ieee11073.sdc.dpws.soap;

import com.google.inject.Inject;
import org.ieee11073.sdc.dpws.soap.exception.SoapFaultException;
import org.ieee11073.sdc.dpws.soap.interception.*;
import org.ieee11073.sdc.dpws.soap.wsaddressing.WsAddressingServerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@linkplain RequestResponseServer}.
 */
public class RequestResponseServerImpl implements RequestResponseServer {
    private static final Logger LOG = LoggerFactory.getLogger(RequestResponseServerImpl.class);

    private final InterceptorRegistry interceptorRegistry;
    private final ServerHelper serverHelper;

    @Inject
    RequestResponseServerImpl(ServerHelper serverHelper,
                              InterceptorRegistry interceptorRegistry,
                              WsAddressingServerInterceptor wsaServerInterceptor) {
        this.serverHelper = serverHelper;
        this.interceptorRegistry = interceptorRegistry;
        register(wsaServerInterceptor);
    }

    @Override
    public void register(Interceptor interceptor) {
        interceptorRegistry.addInterceptor(interceptor);
    }

    @Override
    public InterceptorResult receiveRequestResponse(SoapMessage request,
                                                    SoapMessage response,
                                                    TransportInfo transportInfo) throws SoapFaultException {
        RequestResponseObject rrObj = new RequestResponseObject(request, response, transportInfo);
        InterceptorResult irReq = serverHelper.invokeDispatcher(Direction.REQUEST,
                interceptorRegistry, request, rrObj);

        if (irReq == InterceptorResult.CANCEL || irReq == InterceptorResult.SKIP_RESPONSE) {
            return irReq;
        }

        rrObj = new RequestResponseObject(request, response, transportInfo);
        InterceptorResult irRes = serverHelper.invokeDispatcher(Direction.RESPONSE,
                interceptorRegistry, response, rrObj);

        if (irRes == InterceptorResult.NONE_INVOKED) {
            if (irReq == InterceptorResult.NONE_INVOKED) {
                return InterceptorResult.NONE_INVOKED;
            } else {
                return InterceptorResult.PROCEED;
            }
        }

        return irRes;
        // \todo somewhere here an ActionNotSupported should be thrown
    }
}
