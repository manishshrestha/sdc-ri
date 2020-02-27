package it.org.somda.sdc.dpws;

import com.google.inject.AbstractModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.CommunicationLogImpl;
import org.somda.sdc.dpws.CommunicationLogSink;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.dpws.DpwsTest;
import org.somda.sdc.dpws.TransportBinding;
import org.somda.sdc.dpws.factory.TransportBindingFactory;
import org.somda.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.somda.sdc.dpws.http.apache.ClientTransportBinding;
import org.somda.sdc.dpws.http.jetty.JettyHttpServerHandler;
import org.somda.sdc.dpws.http.jetty.JettyHttpServerRegistry;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(LoggingTestWatcher.class)
public class CommunicationLogIT extends DpwsTest {
    private static final Logger LOG = LoggerFactory.getLogger(CommunicationLogIT.class);

    private TransportBindingFactory transportBindingFactory;
    private SoapMessageFactory soapMessageFactory;
    private EnvelopeFactory envelopeFactory;
    private SoapMarshalling marshalling;
    private TestCommLogSink logSink;
    private JettyHttpServerRegistry httpServerRegistry;

    @BeforeEach
    public void setUp() throws Exception {
        var dpwsOverride = new DefaultDpwsConfigModule() {
            @Override
            public void customConfigure() {
                // ensure commlog works with compression enabled and doesn't store compressed messages
                bind(DpwsConfig.HTTP_GZIP_COMPRESSION, Boolean.class, true);
            }
        };
        var override = new AbstractModule() {
            @Override
            protected void configure() {
                bind(CommunicationLogSink.class).to(TestCommLogSink.class).asEagerSingleton();
                bind(CommunicationLog.class).to(CommunicationLogImpl.class).asEagerSingleton();
            }
        };
        this.overrideBindings(List.of(dpwsOverride, override));
        super.setUp();

        httpServerRegistry = getInjector().getInstance(JettyHttpServerRegistry.class);
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

            // response bytes should exactly match our expected bytes
            assertArrayEquals(expectedResponseStream.toByteArray(), actualResponseStream.toByteArray());

            // requests must contain our message
            var req = logSink.getOutbound().get(0);
            var resp = logSink.getInbound().get(0);

            assertArrayEquals(actualRequestStream.toByteArray(), req.toByteArray());
            assertArrayEquals(expectedResponseStream.toByteArray(), resp.toByteArray());

            // ensure request headers are logged
            assertEquals(
                    ClientTransportBinding.USER_AGENT_VALUE,
                    logSink.getOutboundHeaders().get(0).get(ClientTransportBinding.USER_AGENT_KEY)
            );
            // ensure response headers are logged
            assertEquals(
                    ResponseHandler.TEST_HEADER_VALUE,
                    logSink.getInboundHeaders().get(0).get(ResponseHandler.TEST_HEADER_KEY)
            );

            logSink.clear();
        }
    }

    @Test
    void testServerCommlog() throws Exception {
        URI baseUri = URI.create("http://127.0.0.1:0");
        final String contextPath = "/ctxt/path1";

        final String expectedRequest = "The quick brown fox jumps over the lazy dog";
        final String expectedResponse = "Franz jagt im komplett verwahrlosten Taxi quer durch Bayern";
        AtomicReference<String> resultString = new AtomicReference<>();

        httpServerRegistry.startAsync().awaitRunning();
        URI srvUri1 = httpServerRegistry.registerContext(
                baseUri, contextPath, (req, res, ti) -> {
                    try {
                        byte[] bytes = req.readAllBytes();
                        resultString.set(new String(bytes));

                        // write response
                        res.write(expectedResponse.getBytes());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        // as we explicitly want to set http headers and avoid the commlog, we build our own client
        HttpClient client = HttpClients.custom().setMaxConnPerRoute(1).build();

        // create post request and set custom header
        HttpPost post = new HttpPost(srvUri1);
        String customHeaderKey = "thecustomheader";
        String customHeaderValue = "theCUSTOMvalue";
        post.setHeader(customHeaderKey, customHeaderValue);

        // attach payload
        var requestEntity = new ByteArrayEntity(expectedRequest.getBytes());
        post.setEntity(requestEntity);

        for (int i = 0; i < 100; i++) {
            HttpResponse response = client.execute(post);
            var responseBytes = response.getEntity().getContent().readAllBytes();

            // slurp up any leftover data
            EntityUtils.consume(response.getEntity());

            assertEquals(expectedRequest, resultString.get());
            assertArrayEquals(expectedResponse.getBytes(), responseBytes);

            var req = logSink.getInbound().get(0);
            var resp = logSink.getOutbound().get(0);

            assertArrayEquals(expectedRequest.getBytes(), req.toByteArray());
            assertArrayEquals(expectedResponse.getBytes(), resp.toByteArray());

            // ensure request headers are logged
            assertEquals(
                    customHeaderValue,
                    logSink.getInboundHeaders().get(0).get(customHeaderKey)
            );
            // ensure response headers are logged
            assertEquals(
                    JettyHttpServerHandler.SERVER_HEADER_VALUE,
                    logSink.getOutboundHeaders().get(0).get(JettyHttpServerHandler.SERVER_HEADER_KEY)
            );
            logSink.clear();
        }
    }

    static class TestCommLogSink implements CommunicationLogSink {

        private final ArrayList<ByteArrayOutputStream> inbound;
        private final ArrayList<ByteArrayOutputStream> outbound;
        private final ArrayList<Map<String, String>> inboundHeaders;
        private final ArrayList<Map<String, String>> outboundHeaders;

        TestCommLogSink() {
            this.inbound = new ArrayList<>();
            this.outbound = new ArrayList<>();
            this.inboundHeaders = new ArrayList<>();
            this.outboundHeaders = new ArrayList<>();
        }

        @Override
        public OutputStream getTargetStream(CommunicationLog.TransportType path, CommunicationLog.Direction direction, CommunicationContext communicationContext) {
            var os = new ByteArrayOutputStream();
            var appInfo = (HttpApplicationInfo) communicationContext.getApplicationInfo();
            if (CommunicationLog.Direction.INBOUND.equals(direction)) {
                inbound.add(os);
                inboundHeaders.add(appInfo.getHttpHeaders());
            } else {
                outbound.add(os);
                outboundHeaders.add(appInfo.getHttpHeaders());
            }
            return os;
        }

        public ArrayList<ByteArrayOutputStream> getInbound() {
            return inbound;
        }

        public ArrayList<ByteArrayOutputStream> getOutbound() {
            return outbound;
        }

        public ArrayList<Map<String, String>> getInboundHeaders() {
            return inboundHeaders;
        }

        public ArrayList<Map<String, String>> getOutboundHeaders() {
            return outboundHeaders;
        }

        public void clear() {
            outbound.clear();
            inbound.clear();
            inboundHeaders.clear();
            outboundHeaders.clear();
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
