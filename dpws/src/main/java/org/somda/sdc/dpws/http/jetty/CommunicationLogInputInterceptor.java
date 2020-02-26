package org.somda.sdc.dpws.http.jetty;

import org.eclipse.jetty.server.HttpInput;
import org.eclipse.jetty.util.component.Destroyable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;

public class CommunicationLogInputInterceptor implements HttpInput.Interceptor, Destroyable {
    private static final Logger LOG = LoggerFactory.getLogger(CommunicationLogInputInterceptor.class);

    private final OutputStream commlogStream;

    CommunicationLogInputInterceptor(OutputStream commlogStream) {
        this.commlogStream = commlogStream;
    }

    @Override
    public HttpInput.Content readFrom(HttpInput.Content content) {
        try {
            commlogStream.write(content.getByteBuffer().array());
        } catch (IOException e) {
            LOG.error("Error while writing commlog stream", e);
        }
        return new HttpInput.Content(content.getByteBuffer());
    }

    @Override
    public void destroy() {
        try {
            commlogStream.close();
        } catch (IOException e) {
            LOG.error("Error while closing commlog stream", e);
        }
    }
}
