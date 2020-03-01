package it.org.somda.sdc.dpws.http.jetty;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.dpws.DpwsTest;
import org.somda.sdc.dpws.TransportBinding;
import org.somda.sdc.dpws.TransportBindingException;
import org.somda.sdc.dpws.factory.TransportBindingFactory;
import org.somda.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.somda.sdc.dpws.http.jetty.JettyHttpServerRegistry;
import org.somda.sdc.dpws.soap.SoapMarshalling;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.factory.EnvelopeFactory;
import org.somda.sdc.dpws.soap.factory.SoapMessageFactory;
import test.org.somda.common.LoggingTestWatcher;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@ExtendWith(LoggingTestWatcher.class)
public class JettyHttpServerRegistryIT extends DpwsTest {
    private static final int COMPRESSION_MIN_SIZE = 32;

    private JettyHttpServerRegistry httpServerRegistry;
    private TransportBindingFactory transportBindingFactory;
    private SoapMessageFactory soapMessageFactory;
    private EnvelopeFactory envelopeFactory;

    @BeforeEach
    public void setUp() throws Exception {
        var override = new DefaultDpwsConfigModule() {
            @Override
            public void customConfigure() {
                bind(DpwsConfig.HTTP_GZIP_COMPRESSION, Boolean.class, true);
                bind(DpwsConfig.HTTP_RESPONSE_COMPRESSION_MIN_SIZE, Integer.class, COMPRESSION_MIN_SIZE);
            }
        };
        this.overrideBindings(override);
        super.setUp();

        httpServerRegistry = getInjector().getInstance(JettyHttpServerRegistry.class);
        transportBindingFactory = getInjector().getInstance(TransportBindingFactory.class);
        soapMessageFactory = getInjector().getInstance(SoapMessageFactory.class);
        envelopeFactory = getInjector().getInstance(EnvelopeFactory.class);
        getInjector().getInstance(SoapMarshalling.class).startAsync().awaitRunning();
    }

    @Test
    void registerContext() throws Exception {
        var baseUri = "http://127.0.0.1:0";
        final String ctxtPath = "/test/mulitple/path/segments";
        final AtomicBoolean isRequested = new AtomicBoolean(false);

        httpServerRegistry.startAsync().awaitRunning();
        var srvUri = httpServerRegistry.registerContext(baseUri, ctxtPath, (req, res, ti) -> isRequested.set(true));

        TransportBinding httpBinding = transportBindingFactory.createHttpBinding(srvUri);
        httpBinding.onRequestResponse(createASoapMessage());
        assertThat(isRequested.get(), is(true));
        httpServerRegistry.stopAsync().awaitTerminated();
    }

    @Test
    void unregisterContext() throws Exception {
        var ctxtPath = "/test";
        var baseUri = "http://127.0.0.1:0";
        var isRequested = new AtomicBoolean(false);

        httpServerRegistry.startAsync().awaitRunning();
        var srvUri = httpServerRegistry.registerContext(baseUri, ctxtPath, (req, res, ti) -> isRequested.set(true));

        TransportBinding httpBinding = transportBindingFactory.createHttpBinding(srvUri);
        httpBinding.onNotification(createASoapMessage());
        assertThat(isRequested.get(), is(true));

        isRequested.set(false);
        httpServerRegistry.unregisterContext(srvUri, ctxtPath);

        try {
            httpBinding.onNotification(createASoapMessage());
            fail();
        } catch (TransportBindingException e) {
        }

        assertThat(isRequested.get(), is(false));

        httpServerRegistry.stopAsync().awaitTerminated();
    }

    @Test
    void registerMultipleContextsOnOneServer() throws MarshallingException, TransportException {
        var baseUri = "http://127.0.0.1:0";
        var ctxtPath1 = "/ctxt/path1";
        var ctxtPath2 = "/ctxt/path2";

        var isPath1Requested = new AtomicBoolean(false);
        var isPath2Requested = new AtomicBoolean(false);

        httpServerRegistry.startAsync().awaitRunning();
        var srvUri1 = URI.create(httpServerRegistry.registerContext(baseUri, ctxtPath1, (req, res, ti) ->
                isPath1Requested.set(true)));
        // uri1 has found a free port, attach uri 2 to the same
        baseUri = String.format("%s:%s", srvUri1.getScheme(), srvUri1.getSchemeSpecificPart());
        var srvUri2 = httpServerRegistry.registerContext(baseUri, ctxtPath2, (req, res, ti) ->
                isPath2Requested.set(true));

        TransportBinding httpBinding1 = transportBindingFactory.createHttpBinding(srvUri1.toString());
        TransportBinding httpBinding2 = transportBindingFactory.createHttpBinding(srvUri2);
        httpBinding1.onNotification(createASoapMessage());
        httpBinding2.onNotification(createASoapMessage());
        assertThat(isPath1Requested.get(), is(true));
        assertThat(isPath2Requested.get(), is(true));

        httpServerRegistry.unregisterContext(srvUri1.toString(), srvUri1.getPath());

        isPath1Requested.set(false);
        try {
            httpBinding1.onNotification(createASoapMessage());
            fail();
        } catch (TransportBindingException e) {
        }

        assertThat(isPath1Requested.get(), is(false));

        // verify path 2 is still working after removing path 1
        isPath2Requested.set(false);
        assertFalse(isPath2Requested.get());
        try {
            httpBinding2.onRequestResponse(createASoapMessage());
        } catch (SoapFaultException e) {
            fail(e);
        }
        assertTrue(isPath2Requested.get());

        httpServerRegistry.stopAsync().awaitTerminated();
    }

    @Test
    void registerMultipleServers() {
        var localhostUri = "http://127.0.0.1:0";
        var firstServer = URI.create(httpServerRegistry.initHttpServer(localhostUri));
        var secondServer = URI.create(httpServerRegistry.initHttpServer(localhostUri));
        assertThat(firstServer.getPort(), is(not(secondServer.getPort())));
    }

    @Test
    public void defaultPort() throws Exception {
        // Plausibility test that default port is assigned
        // Plus: there is always one listener expected that holds the port
        // If Jetty is about to change this behavior, this test will fail
        Server httpServer = new Server(new InetSocketAddress("127.0.0.1", 0));
        httpServer.start();
        assertTrue(httpServer.isStarted());
        assertEquals(1, httpServer.getConnectors().length);
        assertThat(httpServer.getURI().getPort(), is(not(0)));
        httpServer.stop();
    }

    @Test
    public void gzipCompression() throws IOException {
        var baseUri = "http://127.0.0.1:0";
        final String compressedPath = "/ctxt/path1";

        final String expectedString = "The quick brown fox jumps over the lazy dog";
        final StringBuilder expectedResponseBuilder = new StringBuilder("ABCEDFG12345");
        // fill up the expected response to match COMPRESSION_MIN_SIZE
        while (expectedResponseBuilder.toString().getBytes().length < COMPRESSION_MIN_SIZE) {
            expectedResponseBuilder.append("a");
        }
        AtomicReference<String> resultString = new AtomicReference<>();

        httpServerRegistry.startAsync().awaitRunning();
        var srvUri1 = httpServerRegistry.registerContext(
                baseUri, compressedPath, (req, res, ti) -> {
                    try {
                        // request should be decompressed transparently
                        byte[] bytes = req.readAllBytes();
                        resultString.set(new String(bytes));

                        // write response, which should be compressed transparently
                        res.write(expectedResponseBuilder.toString().getBytes());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        // as we explicitly want to compress this request, we build our own client
        // with compression disabled, which allows us to validate the response content
        HttpClient client = HttpClients.custom().disableContentCompression().build();

        // create gzip payload
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
            gzos.write(expectedString.getBytes(StandardCharsets.UTF_8));
        }

        byte[] requestContent = baos.toByteArray();
        // ensure data isn't just the raw string
        assertNotEquals(expectedString.getBytes(), requestContent);

        // create post request and set content type to SOAP
        HttpPost post = new HttpPost(srvUri1);
        post.setHeader(HttpHeaders.ACCEPT_ENCODING, "gzip");
        post.setHeader(HttpHeaders.CONTENT_ENCODING, "gzip");

        // attach payload
        var requestEntity = new ByteArrayEntity(requestContent);
        post.setEntity(requestEntity);

        // no retry handling is required as apache httpclient already does
        HttpResponse response = client.execute(post);
        var responseBytes = response.getEntity().getContent().readAllBytes();

        // compressed response should not match uncompressed expectation
        assertNotEquals(expectedResponseBuilder.toString().getBytes(), responseBytes);

        ByteArrayInputStream responseBais = new ByteArrayInputStream(responseBytes);
        byte[] decompressedResponseBytes;
        try (GZIPInputStream gzos = new GZIPInputStream(responseBais)) {
            decompressedResponseBytes = gzos.readAllBytes();
        }

        // decompressed response matches
        assertEquals(expectedResponseBuilder.toString(), new String(decompressedResponseBytes));

        // request was properly processed and decompressed in server
        assertEquals(expectedString, resultString.get());
    }


    private SoapMessage createASoapMessage() {
        return soapMessageFactory.createSoapMessage(envelopeFactory.createEnvelope());
    }
}