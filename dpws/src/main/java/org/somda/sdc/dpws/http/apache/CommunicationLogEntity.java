package org.somda.sdc.dpws.http.apache;

import org.apache.commons.io.input.TeeInputStream;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.http.HttpEntity;
import org.apache.http.entity.HttpEntityWrapper;
import org.somda.sdc.dpws.CommunicationLog;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class CommunicationLogEntity extends HttpEntityWrapper {
    private final OutputStream communicationLogStream;
    private InputStream content;

    /**
     * Creates a new entity wrapper.
     *
     * @param wrappedEntity          the entity to wrap.
     * @param communicationLogStream the stream to also write the content to
     */
    public CommunicationLogEntity(HttpEntity wrappedEntity, OutputStream communicationLogStream) {
        super(wrappedEntity);
        this.communicationLogStream = communicationLogStream;
    }

    @Override
    public InputStream getContent() throws IOException {
        if (wrappedEntity.isStreaming()) {
            if (content == null) {
                content = getWrappedStream();
            }
            return content;
        }
        return getWrappedStream();
    }

    @Override
    public void writeTo(OutputStream outStream) throws IOException {
        var splitOutputStream = new TeeOutputStream(outStream, communicationLogStream);
        super.writeTo(splitOutputStream);
    }

    @Override
    public boolean isRepeatable() {
        return false;
    }

    TeeInputStream getWrappedStream() throws IOException {
        final InputStream in = wrappedEntity.getContent();
        return new TeeInputStream(in, communicationLogStream);
    }
}
