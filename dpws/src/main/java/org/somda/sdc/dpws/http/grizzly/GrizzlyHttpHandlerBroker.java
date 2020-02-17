package org.somda.sdc.dpws.http.grizzly;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.CommunicationLogImpl;
import org.somda.sdc.dpws.http.HttpHandler;
import org.somda.sdc.dpws.soap.TransportInfo;

public class GrizzlyHttpHandlerBroker extends org.glassfish.grizzly.http.server.HttpHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GrizzlyHttpServerRegistry.class);

    private final String mediaType;
    private final HttpHandler handler;
    private final String requestedUri;
    private final CommunicationLog communicationLog;

    @Inject
    GrizzlyHttpHandlerBroker(CommunicationLog communicationLog, @Assisted("mediaType") String mediaType,
            @Assisted HttpHandler handler, @Assisted("requestedUri") String requestedUri) {
        this.mediaType = mediaType;
        this.handler = handler;
        this.requestedUri = requestedUri;
        this.communicationLog = communicationLog;
    }

    @Override
    public void service(Request request, Response response) throws IOException {
        InputStream input = request.getInputStream();

        LOG.debug("Request to {}", requestedUri);
        input = this.communicationLog.logHttpMessage(CommunicationLogImpl.HttpDirection.INBOUND_REQUEST,
                request.getRemoteHost(), request.getRemotePort(), input);

        response.setStatus(HttpStatus.OK_200);
        response.setContentType(mediaType);

        OutputStream output = this.communicationLog.logHttpMessage(CommunicationLogImpl.HttpDirection.OUTBOUND_RESPONSE,
                request.getRemoteHost(), request.getRemotePort(), response.getOutputStream());

        try {

            handler.process(input, output,
                    new TransportInfo(request.getScheme(), request.getLocalAddr(), request.getLocalPort()));

        } catch (Exception e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);

            output.flush();
            output.write(e.getMessage().getBytes());
            output.close();
            LOG.error("Internal server error processing request.", e);
        }
    }
}
