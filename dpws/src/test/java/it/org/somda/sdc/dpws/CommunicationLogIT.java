package it.org.somda.sdc.dpws;

import com.google.inject.AbstractModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.CommunicationLogImpl;
import org.somda.sdc.dpws.CommunicationLogSink;
import org.somda.sdc.dpws.DpwsTest;
import org.somda.sdc.dpws.TransportBinding;
import org.somda.sdc.dpws.factory.TransportBindingFactory;
import org.somda.sdc.dpws.guice.DefaultDpwsModule;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.HttpApplicationInfo;
import org.somda.sdc.dpws.soap.SoapConstants;
import org.somda.sdc.dpws.soap.SoapMarshalling;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.factory.EnvelopeFactory;
import org.somda.sdc.dpws.soap.factory.SoapMessageFactory;
import test.org.somda.common.LoggingTestWatcher;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(LoggingTestWatcher.class)
public class CommunicationLogIT extends DpwsTest {
    private static final Logger LOG = LoggerFactory.getLogger(CommunicationLogIT.class);

    private TransportBindingFactory transportBindingFactory;
    private SoapMessageFactory soapMessageFactory;
    private EnvelopeFactory envelopeFactory;
    private SoapMarshalling marshalling;
    private TestCommLogSink logSink;

    @BeforeEach
    public void setUp() throws Exception {
        var override = new AbstractModule() {
            @Override
            protected void configure() {
                bind(CommunicationLogSink.class).to(TestCommLogSink.class).asEagerSingleton();
                bind(CommunicationLog.class).to(CommunicationLogImpl.class).asEagerSingleton();
            }
        };
        this.overrideBindings(override);
        super.setUp();
        transportBindingFactory = getInjector().getInstance(TransportBindingFactory.class);
        soapMessageFactory = getInjector().getInstance(SoapMessageFactory.class);
        envelopeFactory = getInjector().getInstance(EnvelopeFactory.class);
        marshalling = getInjector().getInstance(SoapMarshalling.class);
        logSink = (TestCommLogSink) getInjector().getInstance(CommunicationLogSink.class);
        marshalling.startAsync().awaitRunning();
    }

    @AfterEach
    void tearDown() throws Exception {
        marshalling.stopAsync().awaitTerminated();
        logSink.clear();
    }


    @Test
    void testClientCommlog() throws Exception {
        URI baseUri = URI.create("http://127.0.0.1:0/");
        String expectedResponse = "Sehr geehrter Kaliba, netter Versuch\n" +
                "Kritische Texte, Weltverbesserer-Blues;";

        JAXBElement<String> jaxbElement = new JAXBElement<>(
                new QName("root-element"),
                String.class, expectedResponse
        );

        var responseEnvelope = createASoapMessage();
        responseEnvelope.getOriginalEnvelope().getBody().getAny().add(jaxbElement);

        // make bytes out of the expected response
        ByteArrayOutputStream expectedResponseStream = new ByteArrayOutputStream();
        marshalling.marshal(responseEnvelope.getEnvelopeWithMappedHeaders(), expectedResponseStream);

        var responseBytes = expectedResponseStream.toByteArray();

        // spawn the http server
        var handler = new ResponseHandler(responseBytes);
        var inetSocketAddress = new InetSocketAddress(baseUri.getHost(), baseUri.getPort());
        var server = HttpServerUtil.spawnHttpServer(inetSocketAddress, handler);

        // replace the port
        baseUri = new URI(
                baseUri.getScheme(),
                baseUri.getUserInfo(),
                baseUri.getHost(),
                server.getAddress().getPort(),
                baseUri.getPath(),
                baseUri.getQuery(),
                baseUri.getFragment());

        // make requests to our server
        TransportBinding httpBinding1 = transportBindingFactory.createHttpBinding(baseUri);
        for (int i = 0; i < 100; i++) {
            var requestMessage = createASoapMessage();

            ByteArrayOutputStream actualRequestStream = new ByteArrayOutputStream();
            marshalling.marshal(requestMessage.getEnvelopeWithMappedHeaders(), actualRequestStream);

            SoapMessage response = httpBinding1.onRequestResponse(requestMessage);

            ByteArrayOutputStream actualResponseStream = new ByteArrayOutputStream();
            marshalling.marshal(response.getEnvelopeWithMappedHeaders(), actualResponseStream);

            // response bytes should exactly match our expected bytes, transparently decompressed
            assertArrayEquals(expectedResponseStream.toByteArray(), actualResponseStream.toByteArray());

            // requests must contain our message
            var req = logSink.getRequests().get(0);
            var resp = logSink.getResponses().get(0);

            assertArrayEquals(actualRequestStream.toByteArray(), req.toByteArray());
            assertArrayEquals(expectedResponseStream.toByteArray(), resp.toByteArray());

            assertEquals(
                    ResponseHandler.TEST_HEADER_VALUE,
                    logSink.getResponseHeaders().get(0).get(ResponseHandler.TEST_HEADER_KEY)
            );

            logSink.clear();
        }
    }

    static class TestCommLogSink implements CommunicationLogSink {

        private final ArrayList<ByteArrayOutputStream> requests;
        private final ArrayList<ByteArrayOutputStream> responses;
        private final ArrayList<Map<String, String>> requestHeaders;
        private final ArrayList<Map<String, String>> responseHeaders;

        TestCommLogSink() {
            this.requests = new ArrayList<>();
            this.responses = new ArrayList<>();
            this.requestHeaders = new ArrayList<>();
            this.responseHeaders = new ArrayList<>();
        }

        @Override
        public OutputStream getTargetStream(CommunicationLog.TransportType path, CommunicationLog.Direction direction, CommunicationContext communicationContext) {
            var os = new ByteArrayOutputStream();
            var appInfo = (HttpApplicationInfo) communicationContext.getApplicationInfo();
            if (CommunicationLog.Direction.INBOUND.equals(direction)) {
                responses.add(os);
                responseHeaders.add(appInfo.getHttpHeaders());
            } else {
                requests.add(os);
                requestHeaders.add(appInfo.getHttpHeaders());
            }
            return os;
        }

        public ArrayList<ByteArrayOutputStream> getRequests() {
            return requests;
        }

        public ArrayList<ByteArrayOutputStream> getResponses() {
            return responses;
        }

        public ArrayList<Map<String, String>> getRequestHeaders() {
            return requestHeaders;
        }

        public ArrayList<Map<String, String>> getResponseHeaders() {
            return responseHeaders;
        }

        public void clear() {
            responses.clear();
            requests.clear();
            requestHeaders.clear();
            responseHeaders.clear();
        }
    }

    static class ResponseHandler implements HttpHandler {
        private static final String TEST_HEADER_KEY = "SDCriTestHeader";
        private static final String TEST_HEADER_VALUE = "anAmazingValue";
        private final byte[] response;

        ResponseHandler(byte[] response) throws IOException {
            this.response = response;
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            t.getResponseHeaders().add(TEST_HEADER_KEY, TEST_HEADER_VALUE);
            t.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, SoapConstants.MEDIA_TYPE_SOAP);
            t.sendResponseHeaders(200, response.length);
            OutputStream os = t.getResponseBody();
            os.write(response);
            os.close();
        }
    }

    private SoapMessage createASoapMessage() {
        return soapMessageFactory.createSoapMessage(envelopeFactory.createEnvelope());
    }
}
