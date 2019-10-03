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
    public void receiveRequestResponse(SoapMessage request,
                                       SoapMessage response,
                                       TransportInfo transportInfo) throws SoapFaultException {
        RequestResponseObject rrObj = new RequestResponseObject(request, response, transportInfo);
        serverHelper.invokeDispatcher(Direction.REQUEST, interceptorRegistry, request, rrObj);

        rrObj = new RequestResponseObject(request, response, transportInfo);
        serverHelper.invokeDispatcher(Direction.RESPONSE, interceptorRegistry, response, rrObj);

        // \todo somewhere here an ActionNotSupported should be thrown
    }
}
