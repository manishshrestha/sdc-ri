package org.ieee11073.sdc.dpws.device.helper;

import com.google.inject.Inject;
import org.ieee11073.sdc.dpws.http.HttpHandler;
import org.ieee11073.sdc.dpws.soap.exception.MarshallingException;
import org.ieee11073.sdc.dpws.soap.exception.SoapFaultException;
import org.ieee11073.sdc.dpws.soap.exception.TransportException;
import org.ieee11073.sdc.dpws.soap.factory.SoapFaultFactory;
import org.ieee11073.sdc.dpws.soap.interception.Interceptor;
import org.ieee11073.sdc.dpws.soap.interception.InterceptorHandler;
import org.ieee11073.sdc.dpws.soap.interception.InterceptorResult;
import org.ieee11073.sdc.dpws.soap.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * {@link RequestResponseServer} that is invoked on {@link HttpHandler} callbacks.
 *
 * The Handler is an {@link InterceptorHandler}. Any registered objects receive the requests delivered by
 * {@link HttpHandler#process(InputStream, OutputStream, TransportInfo)}.
 */
public class RequestResponseServerHttpHandler implements HttpHandler, InterceptorHandler {
    private static final Logger LOG = LoggerFactory.getLogger(RequestResponseServerHttpHandler.class);

    private final RequestResponseServer reqResServer;
    private final MarshallingService marshallingService;
    private final SoapFaultFactory soapFaultFactory;
    private final SoapUtil soapUtil;

    @Inject
    RequestResponseServerHttpHandler(RequestResponseServer reqResServer,
                                     MarshallingService marshallingService,
                                     SoapFaultFactory soapFaultFactory, SoapUtil soapUtil) {
        this.reqResServer = reqResServer;
        this.marshallingService = marshallingService;
        this.soapFaultFactory = soapFaultFactory;
        this.soapUtil = soapUtil;
    }

    @Override
    public void process(InputStream inStream, OutputStream outStream, TransportInfo transportInfo)
            throws TransportException, MarshallingException {
        SoapMessage requestMsg = marshallingService.unmarshal(inStream);
        try {
            inStream.close();
        } catch (IOException e) {
            throw new TransportException("IO error closing HTTP input stream", e);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Incoming SOAP/HTTP request: {}", SoapDebug.get(requestMsg));
        }

        SoapMessage responseMsg = soapUtil.createMessage();
        try {
            InterceptorResult ir = reqResServer.receiveRequestResponse(requestMsg, responseMsg, transportInfo);
            if (ir == InterceptorResult.CANCEL) {
                responseMsg = soapFaultFactory.createReceiverFault("Message processing aborted");
            } else if (ir == InterceptorResult.NONE_INVOKED) {
                responseMsg = soapFaultFactory.createReceiverFault("Message was not processed at the server");
            }
        } catch (SoapFaultException e) {
            responseMsg = e.getFaultMessage();
        }

        try {
            marshallingService.marshal(responseMsg, outStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Outgoing SOAP/HTTP response: {}", SoapDebug.get(responseMsg));
        }

        try {
            outStream.close();
        } catch (IOException e) {
            throw new TransportException("IO error closing HTTP output stream", e);
        }
    }

    @Override
    public void register(Interceptor interceptor) {
        reqResServer.register(interceptor);
    }
}
