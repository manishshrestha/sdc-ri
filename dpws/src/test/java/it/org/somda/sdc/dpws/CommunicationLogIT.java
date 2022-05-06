package it.org.somda.sdc.dpws;

import com.google.common.collect.ListMultimap;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.GZIPContentDecoder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.CommunicationLogImpl;
import org.somda.sdc.dpws.CommunicationLogSink;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.dpws.DpwsTest;
import org.somda.sdc.dpws.TransportBinding;
import org.somda.sdc.dpws.factory.TransportBindingFactory;
import org.somda.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.somda.sdc.dpws.helper.JaxbMarshalling;
import org.somda.sdc.dpws.http.HttpHandler;
import org.somda.sdc.dpws.http.apache.ApacheTransportBindingFactoryImpl;
import org.somda.sdc.dpws.http.apache.ClientTransportBinding;
import org.somda.sdc.dpws.http.jetty.JettyHttpServerHandler;
import org.somda.sdc.dpws.http.jetty.JettyHttpServerRegistry;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.HttpApplicationInfo;
import org.somda.sdc.dpws.soap.SoapMarshalling;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.factory.EnvelopeFactory;
import org.somda.sdc.dpws.soap.factory.SoapMessageFactory;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommunicationLogIT extends DpwsTest {
    private static final Logger LOG = LogManager.getLogger(CommunicationLogIT.class);
    public static final String TRANSFER_ENCODING_HEADER = "transfer-encoding";
    public static final String TRANSFER_ENCODING_VALUE_CHUNKED = "chunked";
    public static final String CONTENT_LENGTH_HEADER = "content-length";
    public static final String CONTENT_ENCODING_HEADER = "content-encoding";
    public static final String X_CONTENT_ENCODING_HEADER = "x-" + CONTENT_ENCODING_HEADER;

    private TransportBindingFactory transportBindingFactory;
    private SoapMessageFactory soapMessageFactory;
    private EnvelopeFactory envelopeFactory;
    private SoapMarshalling marshalling;
    private TestCommLogSink logSink;
    private JettyHttpServerRegistry httpServerRegistry;

    public void setUpChunked() throws Exception {
        var dpwsOverride = new DefaultDpwsConfigModule() {
            @Override
            public void customConfigure() {
                // ensure commlog works with compression enabled and doesn't store compressed messages
                bind(DpwsConfig.HTTP_GZIP_COMPRESSION, Boolean.class, true);
                bind(DpwsConfig.ENFORCE_HTTP_CHUNKED_TRANSFER, Boolean.class, true);
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
        soapMessageFactory = getInjector().getInstance(SoapMessageFactory.class);
        envelopeFactory = getInjector().getInstance(EnvelopeFactory.class);
        getInjector().getInstance(JaxbMarshalling.class).startAsync().awaitRunning();
        marshalling = getInjector().getInstance(SoapMarshalling.class);
        logSink = (TestCommLogSink) getInjector().getInstance(CommunicationLogSink.class);
        marshalling.startAsync().awaitRunning();
        transportBindingFactory = getInjector().getInstance(ApacheTransportBindingFactoryImpl.class);
    }

    public void setUpNonChunked() throws Exception {
        var dpwsOverride = new DefaultDpwsConfigModule() {
            @Override
            public void customConfigure() {
                // ensure commlog works with compression enabled and doesn't store compressed messages
                bind(DpwsConfig.HTTP_GZIP_COMPRESSION, Boolean.class, true);
                bind(DpwsConfig.ENFORCE_HTTP_CHUNKED_TRANSFER, Boolean.class, false);
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
        getInjector().getInstance(JaxbMarshalling.class).startAsync().awaitRunning();
        marshalling = getInjector().getInstance(SoapMarshalling.class);
        logSink = (TestCommLogSink) getInjector().getInstance(CommunicationLogSink.class);
        marshalling.startAsync().awaitRunning();
        transportBindingFactory = getInjector().getInstance(ApacheTransportBindingFactoryImpl.class);
    }

    @AfterEach
    void tearDown() {
        marshalling.stopAsync().awaitTerminated();
        logSink.clear();
    }

    // DONE: check Client: chunked outbound Requests should be logged in commLog with header "Transfer-Encoding: chunked"
    // DONE: check Client: non-chunked outbound Requests should be logged in commLog with header "Content-Length"
    // DONE: check Client: gzipped outbound Requests should be logged in commLog with header "Content-Encoding: gzip"
    // DONE: check Client: chunked inbound Responses should be logged in commLog with header "Transfer-Encoding: chunked"
    // DONE: check Client: non-chunked inbound Responses should be logged in commLog with header "Content-Length"
    // DONE: check Client: gzipped inbound Responses should be logged in commLog with header "Content-Encoding: gzip"
    @Test
    void testClientCommlogChunked() throws Exception {
        setUpChunked();
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
        var expectedResponseStream = new CloseableByteArrayOutputStream();
        marshalling.marshal(responseEnvelope.getEnvelopeWithMappedHeaders(), expectedResponseStream);

        var responseBytes = expectedResponseStream.toByteArray();

        var handler = new HttpServerUtil.GzipResponseHandler(responseBytes, true, false);
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
        TransportBinding httpBinding1 = transportBindingFactory.createHttpBinding(baseUri.toString());

        for (int i = 0; i < 100; i++) {

            var requestMessage = createASoapMessage();

            var actualRequestStream = new CloseableByteArrayOutputStream();
            marshalling.marshal(requestMessage.getEnvelopeWithMappedHeaders(), actualRequestStream);

            SoapMessage response = httpBinding1.onRequestResponse(requestMessage);

            var actualResponseStream = new CloseableByteArrayOutputStream();
            marshalling.marshal(response.getEnvelopeWithMappedHeaders(), actualResponseStream);

            // response bytes should exactly match our expected bytes
            assertArrayEquals(expectedResponseStream.toByteArray(), actualResponseStream.toByteArray());

            // requests must contain our message
            var req = logSink.getOutboundAppLevel().get(0);
            var resp = logSink.getInboundAppLevel().get(0);

            assertArrayEquals(actualRequestStream.toByteArray(), req.toByteArray());
            assertArrayEquals(expectedResponseStream.toByteArray(), resp.toByteArray());
            // TODO: check Network-Level content as well

            // check "Transfer-Encoding: chunked" Header
            // TODO: check Network-Level Headers as well
            assertTrue(logSink.getOutboundAppLevelHeaders().get(0).get(TRANSFER_ENCODING_HEADER).contains(TRANSFER_ENCODING_VALUE_CHUNKED));
            assertTrue(logSink.getInboundAppLevelHeaders().get(0).get(TRANSFER_ENCODING_HEADER).contains(TRANSFER_ENCODING_VALUE_CHUNKED));

            // ensure streams were closed
            assertTrue(req.isClosed());
            assertTrue(resp.isClosed());

            // ensure request headers are logged
            assertTrue(
                    logSink.getOutboundAppLevelHeaders().get(0)
                            .get(ClientTransportBinding.USER_AGENT_KEY.toLowerCase())
                            .contains(ClientTransportBinding.USER_AGENT_VALUE)
            );
            // ensure response headers are logged
            assertTrue(
                    logSink.getInboundAppLevelHeaders().get(0)
                            .get(HttpServerUtil.GzipResponseHandler.TEST_HEADER_KEY.toLowerCase())
                            .contains(HttpServerUtil.GzipResponseHandler.TEST_HEADER_VALUE)
            );

            // all headers must've been converted to lower case, these must be false
            assertFalse(
                    logSink.getOutboundAppLevelHeaders().get(0)
                            .get(ClientTransportBinding.USER_AGENT_KEY)
                            .contains(ClientTransportBinding.USER_AGENT_VALUE)
            );
            assertFalse(
                    logSink.getInboundAppLevelHeaders().get(0)
                            .get(HttpServerUtil.GzipResponseHandler.TEST_HEADER_KEY)
                            .contains(HttpServerUtil.GzipResponseHandler.TEST_HEADER_VALUE)
            );

            assertEquals(CommunicationLog.MessageType.RESPONSE, logSink.getInboundMessageType());
            assertEquals(CommunicationLog.MessageType.REQUEST, logSink.getOutboundMessageType());

            compareTransactionIds(logSink.getInboundTransactionIds(), logSink.getOutboundTransactionIds());

            logSink.clear();
        }
    }

    @Test
    void testClientCommlogNonChunked() throws Exception {
        setUpNonChunked();
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
        var expectedResponseStream = new CloseableByteArrayOutputStream();
        marshalling.marshal(responseEnvelope.getEnvelopeWithMappedHeaders(), expectedResponseStream);

        var responseBytes = expectedResponseStream.toByteArray();

        var handler = new HttpServerUtil.GzipResponseHandler(responseBytes, false, false);
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
        TransportBinding httpBinding1 = transportBindingFactory.createHttpBinding(baseUri.toString());

        for (int i = 0; i < 100; i++) {

            var requestMessage = createASoapMessage();

            var actualRequestStream = new CloseableByteArrayOutputStream();
            marshalling.marshal(requestMessage.getEnvelopeWithMappedHeaders(), actualRequestStream);

            SoapMessage response = httpBinding1.onRequestResponse(requestMessage);

            var actualResponseStream = new CloseableByteArrayOutputStream();
            marshalling.marshal(response.getEnvelopeWithMappedHeaders(), actualResponseStream);

            // response bytes should exactly match our expected bytes
            assertArrayEquals(expectedResponseStream.toByteArray(), actualResponseStream.toByteArray());

            // requests must contain our message
            var req = logSink.getOutboundAppLevel().get(0);
            var resp = logSink.getInboundAppLevel().get(0);

            assertArrayEquals(actualRequestStream.toByteArray(), req.toByteArray());
            assertArrayEquals(expectedResponseStream.toByteArray(), resp.toByteArray());

            // check Content-Length header
            assertTrue(logSink.getOutboundAppLevelHeaders().get(0).get(CONTENT_LENGTH_HEADER).contains(Integer.toString(logSink.getOutboundAppLevel().get(0).toString(StandardCharsets.UTF_8).length())));
            assertTrue(logSink.getInboundAppLevelHeaders().get(0).get(CONTENT_LENGTH_HEADER).contains(Integer.toString(logSink.getInboundAppLevel().get(0).toString(StandardCharsets.UTF_8).length())));

            // ensure streams were closed
            assertTrue(req.isClosed());
            assertTrue(resp.isClosed());

            // ensure request headers are logged
            assertTrue(
                logSink.getOutboundAppLevelHeaders().get(0)
                    .get(ClientTransportBinding.USER_AGENT_KEY.toLowerCase())
                    .contains(ClientTransportBinding.USER_AGENT_VALUE)
            );
            // ensure response headers are logged
            assertTrue(
                logSink.getInboundAppLevelHeaders().get(0)
                    .get(HttpServerUtil.GzipResponseHandler.TEST_HEADER_KEY.toLowerCase())
                    .contains(HttpServerUtil.GzipResponseHandler.TEST_HEADER_VALUE)
            );

            // all headers must've been converted to lower case, these must be false
            assertFalse(
                logSink.getOutboundAppLevelHeaders().get(0)
                    .get(ClientTransportBinding.USER_AGENT_KEY)
                    .contains(ClientTransportBinding.USER_AGENT_VALUE)
            );
            assertFalse(
                logSink.getInboundAppLevelHeaders().get(0)
                    .get(HttpServerUtil.GzipResponseHandler.TEST_HEADER_KEY)
                    .contains(HttpServerUtil.GzipResponseHandler.TEST_HEADER_VALUE)
            );

            assertEquals(CommunicationLog.MessageType.RESPONSE, logSink.getInboundMessageType());
            assertEquals(CommunicationLog.MessageType.REQUEST, logSink.getOutboundMessageType());

            compareTransactionIds(logSink.getInboundTransactionIds(), logSink.getOutboundTransactionIds());

            logSink.clear();
        }
    }

    // TODO: ensure that the App-level Request is never gzip-compressed.
    @Test
    void testClientCommlogGzipped() throws Exception {
        setUpNonChunked();
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
        var expectedResponseStream = new CloseableByteArrayOutputStream();
        marshalling.marshal(responseEnvelope.getEnvelopeWithMappedHeaders(), expectedResponseStream);

        var responseBytes = expectedResponseStream.toByteArray();

        var handler = new HttpServerUtil.GzipResponseHandler(responseBytes, false, true);
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
        TransportBinding httpBinding1 = transportBindingFactory.createHttpBinding(baseUri.toString());
        ((ClientTransportBinding)httpBinding1).setGzippedTransfer(true);

        for (int i = 0; i < 100; i++) {

            var requestMessage = createASoapMessage();

            var actualRequestStream = new CloseableByteArrayOutputStream();
            marshalling.marshal(requestMessage.getEnvelopeWithMappedHeaders(), actualRequestStream);

            SoapMessage response = httpBinding1.onRequestResponse(requestMessage);

            var actualResponseStream = new CloseableByteArrayOutputStream();
            marshalling.marshal(response.getEnvelopeWithMappedHeaders(), actualResponseStream);

            // response bytes should exactly match our expected bytes
            assertArrayEquals(expectedResponseStream.toByteArray(), actualResponseStream.toByteArray());

            // requests must contain our message
            var req = logSink.getOutboundAppLevel().get(0);
            final byte[] netLevelReq = logSink.getOutboundNetLevel().get(0).toByteArray();
            var resp = logSink.getInboundAppLevel().get(0);
            final byte[] netLevelResp = logSink.getInboundNetLevel().get(0).toByteArray();
            assertArrayEquals(expectedResponseStream.toByteArray(), decodeGZip(netLevelResp));
            assertArrayEquals(expectedResponseStream.toByteArray(), resp.toByteArray());
            assertArrayEquals(actualRequestStream.toByteArray(), req.toByteArray());
            assertArrayEquals(actualRequestStream.toByteArray(), decodeGZip(netLevelReq));

            // check Content-Length header
            // Note: when content is gzipped, content-length is not equal to length of content,
            //       hence we can only check if the header is present.
            // TODO: also supply the Header for network- and application-level.
            // TODO: Correct this once network-level and application-level are separated.
            assertNotNull(logSink.getOutboundAppLevelHeaders().get(0).get(CONTENT_LENGTH_HEADER));
            assertNotNull(logSink.getInboundAppLevelHeaders().get(0).get(CONTENT_LENGTH_HEADER));

            // expect both request and response to be gzipped
            assertTrue(logSink.getOutboundAppLevelHeaders().get(0).get(CONTENT_ENCODING_HEADER).contains("gzip"));
            // Problem: When the Client received a gzipped Response from the Server, then the "Content-Encoding: gzip"-Header is stripped in the GzipHandler beneath the
            //          CommLogHandler and hence does not read our Log.
            assertTrue(logSink.getInboundAppLevelHeaders().get(0).get(CONTENT_ENCODING_HEADER).contains("gzip"));

            // ensure streams were closed
            assertTrue(req.isClosed());
            assertTrue(resp.isClosed());

            // ensure request headers are logged
            assertTrue(
                logSink.getOutboundAppLevelHeaders().get(0)
                    .get(ClientTransportBinding.USER_AGENT_KEY.toLowerCase())
                    .contains(ClientTransportBinding.USER_AGENT_VALUE)
            );
            // ensure response headers are logged
            assertTrue(
                logSink.getInboundAppLevelHeaders().get(0)
                    .get(HttpServerUtil.GzipResponseHandler.TEST_HEADER_KEY.toLowerCase())
                    .contains(HttpServerUtil.GzipResponseHandler.TEST_HEADER_VALUE)
            );

            // all headers must've been converted to lower case, these must be false
            assertFalse(
                logSink.getOutboundAppLevelHeaders().get(0)
                    .get(ClientTransportBinding.USER_AGENT_KEY)
                    .contains(ClientTransportBinding.USER_AGENT_VALUE)
            );
            assertFalse(
                logSink.getInboundAppLevelHeaders().get(0)
                    .get(HttpServerUtil.GzipResponseHandler.TEST_HEADER_KEY)
                    .contains(HttpServerUtil.GzipResponseHandler.TEST_HEADER_VALUE)
            );

            assertEquals(CommunicationLog.MessageType.RESPONSE, logSink.getInboundMessageType());
            assertEquals(CommunicationLog.MessageType.REQUEST, logSink.getOutboundMessageType());

            compareTransactionIds(logSink.getInboundTransactionIds(), logSink.getOutboundTransactionIds());

            logSink.clear();
        }
    }

    private byte[] decodeGZip(byte[] netLevelResp) {
        final ByteBuffer decoded = new GZIPContentDecoder().decode(ByteBuffer.wrap(netLevelResp));
        byte[] result = new byte[decoded.limit()];
        decoded.get(result, 0, decoded.limit());
        return result;
    }

    static class SlowInputStream extends InputStream {

        private final byte[] payload;
        private final long delayMs;

        private int nextIndex;

        SlowInputStream(byte[] payload, int sleep) {
            this.payload = payload;
            this.delayMs = sleep;

            this.nextIndex = 0;
        }

        @Override
        public int read() throws IOException {
            // only sleep sometimes
            if (nextIndex % 10000 == 0) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    throw new IOException(e);
                }
            }
            if (payload.length <= nextIndex) {
                return -1;
            }
            return payload[nextIndex++];
        }
    }

    @Test
    void testServerCommlogStreamedRequest() throws Exception {
        setUpChunked();
        var baseUri = "http://127.0.0.1:0";
        var contextPath = "/ctxt/path1";

        final String baseRequest = "The quick brown fox jumps over the lazy dog";
        final String expectedResponse = "Franz jagt im komplett verwahrlosten Taxi quer durch Bayern";
        AtomicReference<String> resultString = new AtomicReference<>();

        httpServerRegistry.startAsync().awaitRunning();
        var srvUri1 = httpServerRegistry.registerContext(
            baseUri, contextPath, new HttpHandler() {
                @Override
                public void handle(InputStream inStream, OutputStream outStream, CommunicationContext communicationContext) {
                    try {
                        byte[] bytes = inStream.readAllBytes();
                        resultString.set(new String(bytes));

                        // write response
                        outStream.write(expectedResponse.getBytes());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });

        // as we explicitly want to set http headers and avoid the commlog, we build our own client
        HttpClient client = HttpClients.custom().setMaxConnPerRoute(1).build();

        // create post request and set custom header
        HttpPost post = new HttpPost(srvUri1);
        String customHeaderKey = "thecustomheader";
        String customHeaderValue = "theCUSTOMvalue";
        post.setHeader(customHeaderKey, customHeaderValue);

        // construct our request payload to be sure it is longer than one chunk
        var expectedRequest = baseRequest.repeat(100000);

        // attach payload
        var requestEntity = new InputStreamEntity(new SlowInputStream(expectedRequest.getBytes(), 50));
        // force chunking because of stream
        requestEntity.setChunked(true);
        post.setEntity(requestEntity);

        for (int i = 0; i < 1; i++) {
            HttpResponse response = client.execute(post);
            var responseBytes = response.getEntity().getContent().readAllBytes();

            // TODO: Because of the Commlog in the server closing after the request is done, we need a little sleep here
            Thread.sleep(10);

            // slurp up any leftover data
            EntityUtils.consume(response.getEntity());

            assertEquals(expectedRequest, resultString.get());
            assertArrayEquals(expectedResponse.getBytes(), responseBytes);

            var req = logSink.getInboundAppLevel().get(0);
            var resp = logSink.getOutboundAppLevel().get(0);

            assertArrayEquals(expectedRequest.getBytes(), req.toByteArray());
            assertArrayEquals(expectedResponse.getBytes(), resp.toByteArray());

            // ensure streams were closed by interceptors
            assertTrue(req.isClosed());
            assertTrue(resp.isClosed());

            // ensure request headers are logged
            assertTrue(
                logSink.getInboundAppLevelHeaders().get(0).get(customHeaderKey)
                    .contains(customHeaderValue)
            );
            // ensure response headers are logged
            assertTrue(
                logSink.getOutboundAppLevelHeaders().get(0)
                    .get(JettyHttpServerHandler.SERVER_HEADER_KEY.toLowerCase())
                    .contains(JettyHttpServerHandler.SERVER_HEADER_VALUE)
            );

            // all headers must've been converted to lower case, these must be false
            assertFalse(
                logSink.getInboundAppLevelHeaders().get(0).get(customHeaderKey.toUpperCase())
                    .contains(customHeaderValue)
            );
            assertFalse(
                logSink.getOutboundAppLevelHeaders().get(0)
                    .get(JettyHttpServerHandler.SERVER_HEADER_KEY)
                    .contains(JettyHttpServerHandler.SERVER_HEADER_VALUE)
            );

            assertEquals(CommunicationLog.MessageType.REQUEST, logSink.getInboundMessageType());
            assertEquals(CommunicationLog.MessageType.RESPONSE, logSink.getOutboundMessageType());

            compareTransactionIds(logSink.getInboundTransactionIds(), logSink.getOutboundTransactionIds());

            logSink.clear();
        }

    }

    // DONE: check Server: chunked inbound Requests should be logged in commLog with header "Transfer-Encoding: chunked"
    // DONE: check Server: non-chunked inbound Requests should be logged in commLog with header "Content-Length"
    // DONE: check Server: gzipped inbound Requests should be logged in commLog with header "Content-Encoding: gzip"
    // DONE: check Server: chunked outbound Responses should be logged in commLog with header "Transfer-Encoding: chunked"
    // DONE: check Server: non-chunked outbound Responses should be logged in commLog with header "Content-Length"
    // DONE: check Server: gzipped outbound Responses should be logged in commLog with header "Content-Encoding: gzip"
    @Test
    void testServerCommlogChunked() throws Exception {
        setUpChunked();
        var baseUri = "http://127.0.0.1:0";
        var contextPath = "/ctxt/path1";

        final String expectedRequest = "The quick brown fox jumps over the lazy dog";
        final String expectedResponse = "Franz jagt im komplett verwahrlosten Taxi quer durch Bayern";
        AtomicReference<String> resultString = new AtomicReference<>();

        httpServerRegistry.startAsync().awaitRunning();
        var srvUri1 = httpServerRegistry.registerContext(
                baseUri, contextPath, new HttpHandler() {
                    @Override
                    public void handle(InputStream inStream, OutputStream outStream, CommunicationContext communicationContext) {
                        try {
                            byte[] bytes = inStream.readAllBytes();
                            resultString.set(new String(bytes));

                            // write response
                            outStream.write(expectedResponse.getBytes());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
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
        // force chunking to test for completeness
        requestEntity.setChunked(true);
        post.setEntity(requestEntity);

        for (int i = 0; i < 10; i++) {
            HttpResponse response = client.execute(post);
            var responseBytes = response.getEntity().getContent().readAllBytes();

            // TODO: Because of the Commlog in the server closing after the request is done, we need a little sleep here
            Thread.sleep(10);

            // slurp up any leftover data
            EntityUtils.consume(response.getEntity());

            assertEquals(expectedRequest, resultString.get());
            assertArrayEquals(expectedResponse.getBytes(), responseBytes);

            var req = logSink.getInboundAppLevel().get(0);
            var resp = logSink.getOutboundAppLevel().get(0);

            assertArrayEquals(expectedRequest.getBytes(), req.toByteArray());
            assertArrayEquals(expectedResponse.getBytes(), resp.toByteArray());

            // ensure streams were closed by interceptors
            assertTrue(req.isClosed());
            assertTrue(resp.isClosed());

            // ensure request headers are logged
            assertTrue(
                    logSink.getInboundAppLevelHeaders().get(0).get(customHeaderKey)
                            .contains(customHeaderValue)
            );
            // the request is chunked, so we expect the transfer-encoding header can be found in the inbound commLog.
            assertTrue(logSink.getInboundAppLevelHeaders().get(0).get("transfer-encoding").contains("chunked"),
                "Expected \"Transfer-Encoding:chunked\"-Header could not be found in Commlog.");

            // the response is chunked, so we expect the transfer-encoding header can be found in the outbound commLog.
            assertTrue(logSink.getOutboundAppLevelHeaders().get(0).get("transfer-encoding").contains("chunked"));

            // ensure response headers are logged
            assertTrue(
                    logSink.getOutboundAppLevelHeaders().get(0)
                            .get(JettyHttpServerHandler.SERVER_HEADER_KEY.toLowerCase())
                            .contains(JettyHttpServerHandler.SERVER_HEADER_VALUE)
            );

            // all headers must've been converted to lower case, these must be false
            assertFalse(
                    logSink.getInboundAppLevelHeaders().get(0).get(customHeaderKey.toUpperCase())
                            .contains(customHeaderValue)
            );
            assertFalse(
                    logSink.getOutboundAppLevelHeaders().get(0)
                            .get(JettyHttpServerHandler.SERVER_HEADER_KEY)
                            .contains(JettyHttpServerHandler.SERVER_HEADER_VALUE)
            );

            assertEquals(CommunicationLog.MessageType.REQUEST, logSink.getInboundMessageType());
            assertEquals(CommunicationLog.MessageType.RESPONSE, logSink.getOutboundMessageType());

            compareTransactionIds(logSink.getInboundTransactionIds(), logSink.getOutboundTransactionIds());

            logSink.clear();
        }
    }

    @Test
    void testServerCommlogNonChunked() throws Exception {
        setUpNonChunked();
        var baseUri = "http://127.0.0.1:0";
        var contextPath = "/ctxt/path1";

        final String expectedRequest = "The quick brown fox jumps over the lazy dog";
        final String expectedResponse = "Franz jagt im komplett verwahrlosten Taxi quer durch Bayern";
        AtomicReference<String> resultString = new AtomicReference<>();

        httpServerRegistry.startAsync().awaitRunning();
        var srvUri1 = httpServerRegistry.registerContext(
            baseUri, contextPath, new HttpHandler() {
                @Override
                public void handle(InputStream inStream, OutputStream outStream, CommunicationContext communicationContext) {
                    try {
                        byte[] bytes = inStream.readAllBytes();
                        resultString.set(new String(bytes));

                        // write response
                        outStream.write(expectedResponse.getBytes());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
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
        // force chunking to test for completeness
        requestEntity.setChunked(false);
        post.setEntity(requestEntity);

        for (int i = 0; i < 10; i++) {
            HttpResponse response = client.execute(post);
            var responseBytes = response.getEntity().getContent().readAllBytes();

            // TODO: Because of the Commlog in the server closing after the request is done, we need a little sleep here
            Thread.sleep(10);

            // slurp up any leftover data
            EntityUtils.consume(response.getEntity());

            assertEquals(expectedRequest, resultString.get());
            assertArrayEquals(expectedResponse.getBytes(), responseBytes);

            var req = logSink.getInboundAppLevel().get(0);
            var resp = logSink.getOutboundAppLevel().get(0);

            assertArrayEquals(expectedRequest.getBytes(), req.toByteArray());
            assertArrayEquals(expectedResponse.getBytes(), resp.toByteArray());

            // ensure streams were closed by interceptors
            assertTrue(req.isClosed());
            assertTrue(resp.isClosed());

            // ensure request headers are logged
            assertTrue(
                logSink.getInboundAppLevelHeaders().get(0).get(customHeaderKey)
                    .contains(customHeaderValue)
            );
            // the request is non-chunked, so we expect the content-length header to be present and correct in the inbound commLog.
            final int actualContentLength = logSink.getInboundAppLevel().get(0).toString(StandardCharsets.UTF_8).length();
            assertEquals(logSink.getInboundAppLevelHeaders().get(0).get(CONTENT_LENGTH_HEADER).get(0), Integer.toString(actualContentLength));

            // the response is non-chunked, so we expect the content-length header to be present and correct in the outbound commLog.
            final int actualContentLength2 = logSink.getOutboundAppLevel().get(0).toString(StandardCharsets.UTF_8).length();
            assertEquals(logSink.getOutboundAppLevelHeaders().get(0).get(CONTENT_LENGTH_HEADER).get(0), Integer.toString(actualContentLength2));

            // ensure response headers are logged
            assertTrue(
                logSink.getOutboundAppLevelHeaders().get(0)
                    .get(JettyHttpServerHandler.SERVER_HEADER_KEY.toLowerCase())
                    .contains(JettyHttpServerHandler.SERVER_HEADER_VALUE)
            );

            // all headers must've been converted to lower case, these must be false
            assertFalse(
                logSink.getInboundAppLevelHeaders().get(0).get(customHeaderKey.toUpperCase())
                    .contains(customHeaderValue)
            );
            assertFalse(
                logSink.getOutboundAppLevelHeaders().get(0)
                    .get(JettyHttpServerHandler.SERVER_HEADER_KEY)
                    .contains(JettyHttpServerHandler.SERVER_HEADER_VALUE)
            );

            assertEquals(CommunicationLog.MessageType.REQUEST, logSink.getInboundMessageType());
            assertEquals(CommunicationLog.MessageType.RESPONSE, logSink.getOutboundMessageType());

            compareTransactionIds(logSink.getInboundTransactionIds(), logSink.getOutboundTransactionIds());

            logSink.clear();
        }
    }

    @Test
    void testServerCommlogGzipped() throws Exception {
        setUpNonChunked(); // Note that Gzip-compressed Entities are always chunked.
        var baseUri = "http://127.0.0.1:0";
        var contextPath = "/ctxt/path1";

        final String expectedRequest = "The quick brown fox jumps over the lazy dog";
        final String expectedResponse = "Franz jagt im komplett verwahrlosten Taxi quer durch Bayern";
        AtomicReference<String> resultString = new AtomicReference<>();

        httpServerRegistry.startAsync().awaitRunning();
        var srvUri1 = httpServerRegistry.registerContext(
            baseUri, contextPath, new HttpHandler() {
                @Override
                public void handle(InputStream inStream, OutputStream outStream, CommunicationContext communicationContext) {
                    try {
                        byte[] bytes = inStream.readAllBytes();
                        resultString.set(new String(bytes));

                        // write response
                        outStream.write(expectedResponse.getBytes());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
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
        var requestEntity = EntityBuilder.create()
                                                   .setBinary(expectedRequest.getBytes())
                                                   .gzipCompress()
                                                   .build();
        post.setEntity(requestEntity);


        for (int i = 0; i < 10; i++) {
            HttpResponse response = client.execute(post);
            var responseBytes = response.getEntity().getContent().readAllBytes();

            // TODO: Because of the Commlog in the server closing after the request is done, we need a little sleep here
            Thread.sleep(10);

            // slurp up any leftover data
            EntityUtils.consume(response.getEntity());

            assertEquals(expectedRequest, resultString.get());
            assertArrayEquals(expectedResponse.getBytes(), responseBytes);

            var req = logSink.getInboundAppLevel().get(0);
            var resp = logSink.getOutboundAppLevel().get(0);

            assertArrayEquals(expectedRequest.getBytes(), req.toByteArray());
            assertArrayEquals(expectedResponse.getBytes(), resp.toByteArray());

            // expect request and response to be gzipped
            // TODO: store both the headers before GzipHandler and the headers after GzipHandler
            // TODO: assertTrue(logSink.getInboundNetworkHeaders().get(0).get(CONTENT_ENCODING_HEADER).contains("gzip"));
            assertTrue(logSink.getInboundAppLevelHeaders().get(0).get(X_CONTENT_ENCODING_HEADER).contains("gzip"));

            // TODO: assertTrue(logSink.getOutboundNetworkHeaders().get(0).get(CONTENT_ENCODING_HEADER).contains("gzip"));
            // TODO: does the Content-Encoding Header appear before the GzipHandler?
            assertTrue(logSink.getOutboundAppLevelHeaders().get(0).get(CONTENT_ENCODING_HEADER).contains("gzip"));

            // ensure streams were closed by interceptors
            assertTrue(req.isClosed());
            assertTrue(resp.isClosed());

            // ensure request headers are logged
            assertTrue(
                logSink.getInboundAppLevelHeaders().get(0).get(customHeaderKey)
                    .contains(customHeaderValue)
            );
            // ensure response headers are logged
            assertTrue(
                logSink.getOutboundAppLevelHeaders().get(0)
                    .get(JettyHttpServerHandler.SERVER_HEADER_KEY.toLowerCase())
                    .contains(JettyHttpServerHandler.SERVER_HEADER_VALUE)
            );

            // all headers must've been converted to lower case, these must be false
            assertFalse(
                logSink.getInboundAppLevelHeaders().get(0).get(customHeaderKey.toUpperCase())
                    .contains(customHeaderValue)
            );
            assertFalse(
                logSink.getOutboundAppLevelHeaders().get(0)
                    .get(JettyHttpServerHandler.SERVER_HEADER_KEY)
                    .contains(JettyHttpServerHandler.SERVER_HEADER_VALUE)
            );

            assertEquals(CommunicationLog.MessageType.REQUEST, logSink.getInboundMessageType());
            assertEquals(CommunicationLog.MessageType.RESPONSE, logSink.getOutboundMessageType());

            compareTransactionIds(logSink.getInboundTransactionIds(), logSink.getOutboundTransactionIds());

            logSink.clear();
        }
    }

    @Test
    void testServerCommlogDuplicateKeys() throws Exception {
        setUpChunked();
        var baseUri = "http://127.0.0.1:0";
        var contextPath = "/ctxt/path1";

        final String expectedRequest = "The quick brown fox jumps over the lazy dog";
        final String expectedResponse = "Franz jagt im komplett verwahrlosten Taxi quer durch Bayern";
        AtomicReference<String> resultString = new AtomicReference<>();

        httpServerRegistry.startAsync().awaitRunning();
        var srvUri1 = httpServerRegistry.registerContext(
                baseUri, contextPath, new HttpHandler() {
                    @Override
                    public void handle(InputStream inStream, OutputStream outStream, CommunicationContext communicationContext) {
                        try {
                            byte[] bytes = inStream.readAllBytes();
                            resultString.set(new String(bytes));

                            // write response
                            outStream.write(expectedResponse.getBytes());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });

        // as we explicitly want to set http headers and avoid the commlog, we build our own client
        HttpClient client = HttpClients.custom().setMaxConnPerRoute(1).build();

        // create post request and set custom header
        HttpPost post = new HttpPost(srvUri1);
        String customHeaderKey = "thecustomheader";
        String customHeaderValue = "theCUSTOMvalue";
        String customHeaderValue2 = "theCUSTOMvalue2";
        post.setHeader(customHeaderKey, customHeaderValue);
        // ensure an upper case value will be mapped to the same key
        post.addHeader(customHeaderKey.toUpperCase(), customHeaderValue2);

        // attach payload
        var requestEntity = new ByteArrayEntity(expectedRequest.getBytes());
        post.setEntity(requestEntity);

        for (int i = 0; i < 100; i++) {
            HttpResponse response = client.execute(post);
            var responseBytes = response.getEntity().getContent().readAllBytes();

            // TODO: Because of the Commlog in the server closing after the request is done, we need a little sleep here
            Thread.sleep(10);

            // slurp up any leftover data
            EntityUtils.consume(response.getEntity());

            assertEquals(expectedRequest, resultString.get());
            assertArrayEquals(expectedResponse.getBytes(), responseBytes);

            var req = logSink.getInboundAppLevel().get(0);
            var resp = logSink.getOutboundAppLevel().get(0);

            assertArrayEquals(expectedRequest.getBytes(), req.toByteArray());
            assertArrayEquals(expectedResponse.getBytes(), resp.toByteArray());

            // ensure request headers are logged
            assertEquals(
                    2,
                    logSink.getInboundAppLevelHeaders().get(0).get(customHeaderKey).size()
            );
            assertTrue(
                    logSink.getInboundAppLevelHeaders().get(0).get(customHeaderKey)
                            .contains(customHeaderValue)
            );
            assertTrue(
                    logSink.getInboundAppLevelHeaders().get(0).get(customHeaderKey)
                            .contains(customHeaderValue2)
            );
            // assert classic behavior does concat
            assertEquals(customHeaderValue + "," + customHeaderValue2,
                    logSink.inboundAppLevelHeadersOld.get(0).get(customHeaderKey.toLowerCase()));
            logSink.clear();
        }
    }

    @Test
    void testDuplicateTransactionIds() throws Exception {
        setUpChunked();
        final Injector secondInjector = configureInjector(List.of(
                new DefaultDpwsConfigModule() {
                    @Override
                    public void customConfigure() {
                        // ensure commlog works with compression enabled and doesn't store compressed messages
                        bind(DpwsConfig.HTTP_GZIP_COMPRESSION, Boolean.class, true);
                    }
                },
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(CommunicationLogSink.class).toInstance(logSink);
                        bind(CommunicationLog.class).to(CommunicationLogImpl.class).asEagerSingleton();
                    }
                }
        ));

        final JettyHttpServerRegistry secondHttpServerRegistry = secondInjector.getInstance(JettyHttpServerRegistry.class);
        final TransportBindingFactory secondTransportBindingFactory = secondInjector.getInstance(TransportBindingFactory.class);
        final SoapMessageFactory secondSoapMessageFactory = secondInjector.getInstance(SoapMessageFactory.class);
        final EnvelopeFactory secondEnvelopeFactory = secondInjector.getInstance(EnvelopeFactory.class);
        secondInjector.getInstance(JaxbMarshalling.class).startAsync().awaitRunning();
        final TestCommLogSink secondLogSink = (TestCommLogSink) secondInjector.getInstance(CommunicationLogSink.class);
        final SoapMarshalling secondMarshalling = secondInjector.getInstance(SoapMarshalling.class);
        secondMarshalling.startAsync().awaitRunning();


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
        var expectedResponseStream = new CloseableByteArrayOutputStream();
        marshalling.marshal(responseEnvelope.getEnvelopeWithMappedHeaders(), expectedResponseStream);

        var responseBytes = expectedResponseStream.toByteArray();

        // spawn the http server
        var handler = new HttpServerUtil.GzipResponseHandler(responseBytes);
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
        TransportBinding httpBinding1 = transportBindingFactory.createHttpBinding(baseUri.toString());

        TransportBinding httpBinding2 = secondTransportBindingFactory.createHttpBinding(baseUri.toString());
        for (int i = 0; i < 100; i++) {

            testSharedLogSink(marshalling, httpBinding1, logSink, expectedResponseStream);
            testSharedLogSink(secondMarshalling, httpBinding2, secondLogSink, expectedResponseStream);

            assertEquals(CommunicationLog.MessageType.RESPONSE, logSink.getInboundMessageType());
            assertEquals(CommunicationLog.MessageType.REQUEST, logSink.getOutboundMessageType());

            compareTransactionIds(logSink.getInboundTransactionIds(), logSink.getOutboundTransactionIds());

            logSink.clear();
        }
    }

    private void testSharedLogSink(SoapMarshalling soapMarshalling, TransportBinding httpBinding, TestCommLogSink logSink, ByteArrayOutputStream expectedResponseStream) throws Exception {
        var requestMessage2 = createASoapMessage();

        var actualRequestStream2 = new CloseableByteArrayOutputStream();
        soapMarshalling.marshal(requestMessage2.getEnvelopeWithMappedHeaders(), actualRequestStream2);

        SoapMessage response2 = httpBinding.onRequestResponse(requestMessage2);

        var actualResponseStream2 = new CloseableByteArrayOutputStream();
        soapMarshalling.marshal(response2.getEnvelopeWithMappedHeaders(), actualResponseStream2);

        // response bytes should exactly match our expected bytes
        assertArrayEquals(expectedResponseStream.toByteArray(), actualResponseStream2.toByteArray());

        // requests must contain our message
        var req2 = logSink.getOutboundAppLevel().get(0);
        var resp2 = logSink.getInboundAppLevel().get(0);

        assertArrayEquals(actualRequestStream2.toByteArray(), req2.toByteArray());
        assertArrayEquals(expectedResponseStream.toByteArray(), resp2.toByteArray());

        // ensure request headers are logged
        assertTrue(
                logSink.getOutboundAppLevelHeaders().get(0)
                        .get(ClientTransportBinding.USER_AGENT_KEY.toLowerCase())
                        .contains(ClientTransportBinding.USER_AGENT_VALUE)
        );
        // ensure response headers are logged
        assertTrue(
                logSink.getInboundAppLevelHeaders().get(0)
                        .get(HttpServerUtil.GzipResponseHandler.TEST_HEADER_KEY.toLowerCase())
                        .contains(HttpServerUtil.GzipResponseHandler.TEST_HEADER_VALUE)
        );

        // all headers must've been converted to lower case, these must be false
        assertFalse(
                logSink.getOutboundAppLevelHeaders().get(0)
                        .get(ClientTransportBinding.USER_AGENT_KEY)
                        .contains(ClientTransportBinding.USER_AGENT_VALUE)
        );
        assertFalse(
                logSink.getInboundAppLevelHeaders().get(0)
                        .get(HttpServerUtil.GzipResponseHandler.TEST_HEADER_KEY)
                        .contains(HttpServerUtil.GzipResponseHandler.TEST_HEADER_VALUE)
        );
    }

    private void compareTransactionIds(ArrayList<String> inboundTransactionIds, ArrayList<String> outboundTransactionIds) {
        final var inboundSet = new HashSet<>(inboundTransactionIds);
        final var outboundSet = new HashSet<>(outboundTransactionIds);

        //check uniqueness of transaction ids
        assertEquals(inboundTransactionIds.size(), inboundSet.size(), "Duplicate Inbound TransactionIds.");
        assertEquals(outboundTransactionIds.size(), outboundSet.size(), "Duplicate Outbound TransactionIds.");

        //every request has a response
        assertEquals(inboundSet, outboundSet, "Not all responses are associated with a request.");
    }

    static class CloseableByteArrayOutputStream extends ByteArrayOutputStream {
        private boolean closed;

        CloseableByteArrayOutputStream() {
            this.closed = false;
        }

        @Override
        public void close() throws IOException {
            super.close();
            this.closed = true;
        }

        private void ensureNotClosed() {
            if (closed) {
                throw new RuntimeException("Stream closed");
            }
        }

        @Override
        public synchronized void write(final int b) {
            ensureNotClosed();
            super.write(b);
        }

        @Override
        public synchronized void write(final byte[] b, final int off, final int len) {
            ensureNotClosed();
            super.write(b, off, len);
        }

        @Override
        public void write(final byte[] b) throws IOException {
            ensureNotClosed();
            super.write(b);
        }

        public boolean isClosed() {
            return closed;
        }
    }

    static class TestCommLogSink implements CommunicationLogSink {

        private final ArrayList<CloseableByteArrayOutputStream> inboundAppLevel;
        private final ArrayList<CloseableByteArrayOutputStream> outboundAppLevel;
        private final ArrayList<ListMultimap<String, String>> inboundAppLevelHeaders;
        private final ArrayList<ListMultimap<String, String>> outboundAppLevelHeaders;
        private final ArrayList<Map<String, String>> inboundAppLevelHeadersOld;
        private final ArrayList<Map<String, String>> outboundAppLevelHeadersOld;
        private CommunicationLog.MessageType inboundMessageType;
        private CommunicationLog.MessageType outboundMessageType;
        private final ArrayList<String> inboundTransactionIds;
        private final ArrayList<String> outboundTransactionIds;

        private final ArrayList<CloseableByteArrayOutputStream> inboundNetLevel;
        private final ArrayList<CloseableByteArrayOutputStream> outboundNetLevel;
        private final ArrayList<ListMultimap<String, String>> inboundNetLevelHeaders;
        private final ArrayList<ListMultimap<String, String>> outboundNetLevelHeaders;
        private final ArrayList<Map<String, String>> inboundNetLevelHeadersOld;
        private final ArrayList<Map<String, String>> outboundNetLevelHeadersOld;

        TestCommLogSink() {
            // App Level
            this.inboundAppLevel = new ArrayList<>();
            this.outboundAppLevel = new ArrayList<>();
            this.inboundAppLevelHeaders = new ArrayList<>();
            this.outboundAppLevelHeaders = new ArrayList<>();
            this.inboundAppLevelHeadersOld = new ArrayList<>();
            this.outboundAppLevelHeadersOld = new ArrayList<>();

            // Net Level
            this.inboundNetLevel = new ArrayList<>();
            this.outboundNetLevel = new ArrayList<>();
            this.inboundNetLevelHeaders = new ArrayList<>();
            this.outboundNetLevelHeaders = new ArrayList<>();
            this.inboundNetLevelHeadersOld = new ArrayList<>();
            this.outboundNetLevelHeadersOld = new ArrayList<>();

            // general
            this.inboundTransactionIds = new ArrayList<>();
            this.outboundTransactionIds = new ArrayList<>();
        }

        @Override
        public OutputStream createTargetStream(CommunicationLog.TransportType path,
                                               CommunicationLog.Direction direction,
                                               CommunicationLog.MessageType messageType,
                                               CommunicationContext communicationContext,
                                               CommunicationLog.Level level) {
            var os = new CloseableByteArrayOutputStream();
            var appInfo = (HttpApplicationInfo) communicationContext.getApplicationInfo();
            if (CommunicationLog.Direction.INBOUND.equals(direction)) {
                if (CommunicationLog.Level.APPLICATION.equals(level)) {
                    inboundAppLevel.add(os);
                    inboundAppLevelHeaders.add(appInfo.getHeaders());
                    inboundAppLevelHeadersOld.add(appInfo.getHttpHeaders());
                    inboundMessageType = messageType;
                    inboundTransactionIds.add(
                        ((HttpApplicationInfo) communicationContext.getApplicationInfo()).getTransactionId());
                } else {
                    inboundNetLevel.add(os);
                    inboundNetLevelHeaders.add(appInfo.getHeaders());
                    inboundNetLevelHeadersOld.add(appInfo.getHttpHeaders());
                    inboundMessageType = messageType;
                }
            } else {
                if (CommunicationLog.Level.APPLICATION.equals(level)) {
                    outboundAppLevel.add(os);
                    outboundAppLevelHeaders.add(appInfo.getHeaders());
                    outboundAppLevelHeadersOld.add(appInfo.getHttpHeaders());
                    outboundMessageType = messageType;
                    outboundTransactionIds.add(
                        ((HttpApplicationInfo) communicationContext.getApplicationInfo()).getTransactionId());
                } else {
                    outboundNetLevel.add(os);
                    outboundNetLevelHeaders.add(appInfo.getHeaders());
                    outboundNetLevelHeadersOld.add(appInfo.getHttpHeaders());
                    outboundMessageType = messageType;
                }
            }
            return os;
        }

        public ArrayList<CloseableByteArrayOutputStream> getInboundAppLevel() { return inboundAppLevel; }

        public ArrayList<CloseableByteArrayOutputStream> getOutboundAppLevel() {
            return outboundAppLevel;
        }

        public ArrayList<CloseableByteArrayOutputStream> getInboundNetLevel() { return inboundNetLevel; }

        public ArrayList<CloseableByteArrayOutputStream> getOutboundNetLevel() {
            return outboundNetLevel;
        }

        public ArrayList<ListMultimap<String, String>> getInboundAppLevelHeaders() {
            return inboundAppLevelHeaders;
        }

        public ArrayList<ListMultimap<String, String>> getOutboundAppLevelHeaders() {
            return outboundAppLevelHeaders;
        }

        public ArrayList<ListMultimap<String, String>> getInboundNetLevelHeaders() {
            return inboundNetLevelHeaders;
        }

        public ArrayList<ListMultimap<String, String>> getOutboundNetLevelHeaders() {
            return outboundNetLevelHeaders;
        }

        public ArrayList<Map<String, String>> getInboundAppLevelHeadersOld() {
            return inboundAppLevelHeadersOld;
        }

        public ArrayList<Map<String, String>> getOutboundAppLevelHeadersOld() {
            return outboundAppLevelHeadersOld;
        }

        public ArrayList<Map<String, String>> getInboundNetLevelHeadersOld() {
            return inboundNetLevelHeadersOld;
        }

        public ArrayList<Map<String, String>> getOutboundNetLevelHeadersOld() {
            return outboundNetLevelHeadersOld;
        }

        public void clear() {
            outboundAppLevel.clear();
            inboundAppLevel.clear();
            inboundAppLevelHeaders.clear();
            outboundAppLevelHeaders.clear();
            inboundAppLevelHeadersOld.clear();
            outboundAppLevelHeadersOld.clear();

            outboundNetLevel.clear();
            inboundNetLevel.clear();
            inboundNetLevelHeaders.clear();
            outboundNetLevelHeaders.clear();
            inboundNetLevelHeadersOld.clear();
            outboundNetLevelHeadersOld.clear();

            // TODO: why are the TransactionIDs not cleared?
        }

        public CommunicationLog.MessageType getOutboundMessageType() {
            return outboundMessageType;
        }

        public CommunicationLog.MessageType getInboundMessageType() {
            return inboundMessageType;
        }

        public ArrayList<String> getInboundTransactionIds() {
            return inboundTransactionIds;
        }

        public ArrayList<String> getOutboundTransactionIds() {
            return outboundTransactionIds;
        }
    }

    private SoapMessage createASoapMessage() {
        return soapMessageFactory.createSoapMessage(envelopeFactory.createEnvelope());
    }
}
