package org.somda.sdc.dpws.device.helper;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.dpws.http.HttpException;
import org.somda.sdc.dpws.http.HttpHandler;
import org.somda.sdc.dpws.soap.*;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.interception.Interceptor;
import org.somda.sdc.dpws.soap.interception.InterceptorHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * {@linkplain RequestResponseServer} that is invoked on SOAP {@linkplain HttpHandler} callbacks.
 * <p>
 * The handler is an {@link InterceptorHandler}.
 * All objects registered via {@link #register(Interceptor)} receive the requests delivered to
 * {@link HttpHandler#process(InputStream, OutputStream, CommunicationContext)}.
 */
public class RequestResponseServerHttpHandler implements HttpHandler, InterceptorHandler {
    private static final Logger LOG = LoggerFactory.getLogger(RequestResponseServerHttpHandler.class);

    private final RequestResponseServer reqResServer;
    private final MarshallingService marshallingService;
    private final SoapUtil soapUtil;

    @Inject
    RequestResponseServerHttpHandler(RequestResponseServer reqResServer,
                                     MarshallingService marshallingService,
                                     SoapUtil soapUtil) {
        this.reqResServer = reqResServer;
        this.marshallingService = marshallingService;
        this.soapUtil = soapUtil;
    }

    @Override
    public void process(InputStream inStream, OutputStream outStream, CommunicationContext communicationContext)
            throws TransportException, HttpException {

        SoapMessage requestMsg;
        try {
            requestMsg = marshallingService.unmarshal(inStream);
        } catch (MarshallingException e) {
            throw new HttpException(500, String.format("Error unmarshalling HTTP input stream: %s", e.getMessage()));
        }

        try {
            inStream.close();
        } catch (IOException e) {
            throw new TransportException("IO error closing HTTP input stream", e);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Incoming SOAP/HTTP request: {}", SoapDebug.get(requestMsg));
        }

        SoapMessage responseMsg = soapUtil.createMessage();
        HttpException httpExceptionToThrow = null;
        try {
            reqResServer.receiveRequestResponse(requestMsg, responseMsg, communicationContext);
        } catch (SoapFaultException e) {
            responseMsg = e.getFaultMessage();
            httpExceptionToThrow = new HttpException(SoapFaultHttpStatusCodeMapping.get(e.getFault()));
        }

        try {
            marshallingService.marshal(responseMsg, outStream);
        } catch (MarshallingException e) {
            throw new HttpException(500, String.format("Error marshalling HTTP output stream: %s", e.getMessage()));
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Outgoing SOAP/HTTP response: {}", SoapDebug.get(responseMsg));
        }

        try {
            outStream.close();
        } catch (IOException e) {
            throw new TransportException("IO error closing HTTP output stream", e);
        }

        if (httpExceptionToThrow != null) {
            throw httpExceptionToThrow;
        }
    }

    @Override
    public void register(Interceptor interceptor) {
        reqResServer.register(interceptor);
    }
}
