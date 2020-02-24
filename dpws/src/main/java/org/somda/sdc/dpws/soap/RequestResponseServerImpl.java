package org.somda.sdc.dpws.soap;

import com.google.inject.Inject;
import org.somda.sdc.dpws.guice.DeviceSpecific;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.interception.*;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingServerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@linkplain RequestResponseServer}.
 */
public class RequestResponseServerImpl implements RequestResponseServer {
    private static final Logger LOG = LoggerFactory.getLogger(RequestResponseServerImpl.class);

    private final InterceptorRegistry interceptorRegistry;
    private final ServerDispatcher serverDispatcher;

    @Inject
    RequestResponseServerImpl(ServerDispatcher serverDispatcher,
                              InterceptorRegistry interceptorRegistry,
                              @DeviceSpecific WsAddressingServerInterceptor wsaServerInterceptor) {
        this.serverDispatcher = serverDispatcher;
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
        serverDispatcher.invokeDispatcher(Direction.REQUEST, interceptorRegistry, request, rrObj);

        rrObj = new RequestResponseObject(request, response, transportInfo);
        serverDispatcher.invokeDispatcher(Direction.RESPONSE, interceptorRegistry, response, rrObj);

        // \todo somewhere here an ActionNotSupported should be thrown
    }
}
