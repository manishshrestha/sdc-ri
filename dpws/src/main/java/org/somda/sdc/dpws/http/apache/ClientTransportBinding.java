package org.somda.sdc.dpws.http.apache;

import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.TransportBinding;
import org.somda.sdc.dpws.TransportBindingException;
import org.somda.sdc.dpws.soap.SoapConstants;
import org.somda.sdc.dpws.soap.SoapMarshalling;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.URI;

public class ClientTransportBinding implements TransportBinding {
    private static final Logger LOG = LoggerFactory.getLogger(ClientTransportBinding.class);

    public static final String USER_AGENT_KEY = "X-User-Agent";
    public static final String USER_AGENT_VALUE = "SDCri";

    private final SoapMarshalling marshalling;
    private final SoapUtil soapUtil;
    private HttpClient client;
    private final URI clientUri;
    private CommunicationLog communicationLog;

    @Inject
    ClientTransportBinding(@Assisted HttpClient client,
                           @Assisted URI clientUri,
                           @Assisted SoapMarshalling marshalling,
                           @Assisted SoapUtil soapUtil,
                           CommunicationLog communicationLog) {
        LOG.debug("Creating ClientTransportBinding for {}", clientUri);
        this.client = client;
        this.clientUri = clientUri;
        this.marshalling = marshalling;
        this.soapUtil = soapUtil;
        this.communicationLog = communicationLog;
    }

    @Override
    public void onNotification(SoapMessage notification) throws TransportBindingException {
        // Ignore the result even if there is one
        try {
            onRequestResponse(notification);
        } catch (SoapFaultException e) {
            // Swallow exception, rationale:
            // we assume that notifications have no response and therefore no soap exception
            // that could be thrown
        }
    }

    @Override
    public SoapMessage onRequestResponse(SoapMessage request) throws TransportBindingException, SoapFaultException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        // create post request and set content type to SOAP
        HttpPost post = new HttpPost(this.clientUri);
        post.setHeader(HttpHeaders.ACCEPT, SoapConstants.MEDIA_TYPE_SOAP);
        post.setHeader(HttpHeaders.CONTENT_TYPE, SoapConstants.MEDIA_TYPE_SOAP);
        post.setHeader(USER_AGENT_KEY, USER_AGENT_VALUE);

        try {
            marshalling.marshal(request.getEnvelopeWithMappedHeaders(), byteArrayOutputStream);
        } catch (JAXBException e) {
            LOG.warn("Marshalling of a message failed: {}", e.getMessage());
            LOG.trace("Marshalling of a message failed", e);
            throw new TransportBindingException(
                    String.format("Sending of a request failed due to marshalling problem: %s", e.getMessage()));
        }

        // attach payload
        var requestEntity = new ByteArrayEntity(byteArrayOutputStream.toByteArray());
        post.setEntity(requestEntity);

        LOG.debug("Sending POST request to {}", this.clientUri);
        HttpResponse response;

        try {
            // no retry handling is required as apache httpclient already does
            response = this.client.execute(post);
        } catch (SocketException e) {
            LOG.error("No response received in request to {}", this.clientUri, e);
            throw new TransportBindingException(e);
        } catch (IOException e) {
            LOG.error("Unexpected IO exception on request to {}", this.clientUri);
            LOG.trace("Unexpected IO exception on request to {}", this.clientUri, e);
            throw new TransportBindingException("No response received");
        }

        if (response.getStatusLine().getStatusCode() >= 300) {
            throw new TransportBindingException(String.format(
                    "Endpoint was not able to process request. HTTP status code: %s", response.getStatusLine()));
        }

        HttpEntity entity = response.getEntity();
        byte[] bytes;

        try (InputStream contentStream = entity.getContent()) {
            bytes = ByteStreams.toByteArray(contentStream);
        } catch (IOException e) {
            LOG.error("Couldn't read response", e);
            bytes = new byte[0];
        }

        try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
            if (inputStream.available() > 0) {
                SoapMessage msg = soapUtil.createMessage(marshalling.unmarshal(inputStream));
                if (msg.isFault()) {
                    throw new SoapFaultException(msg);
                }

                return msg;
            }
        } catch (JAXBException e) {
            LOG.debug("Unmarshalling of a message failed: {}", e.getMessage());
            LOG.trace("Unmarshalling of a message failed.", e);
            throw new TransportBindingException(String
                    .format("Receiving of a response failed due to unmarshalling problem: %s", e.getMessage()));
        } catch (IOException e) {
            LOG.debug("Error occurred while processing response: {}", e.getMessage());
            LOG.trace("Error occurred while processing response", e);
        } finally {
            try {
                // ensure the entire response was consumed, just in case
                EntityUtils.consume(response.getEntity());
            } catch (IOException e) {
                // if this fails, we will either all die or it doesn't matter at all...
            }
        }

        return soapUtil.createMessage();
    }

    @Override
    public void close() {
        // no action on HTTP
    }
}
