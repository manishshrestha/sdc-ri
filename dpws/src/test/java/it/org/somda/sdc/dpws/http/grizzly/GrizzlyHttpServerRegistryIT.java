package it.org.somda.sdc.dpws.http.grizzly;

import com.google.common.collect.Iterables;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClients;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.dpws.DpwsTest;
import org.somda.sdc.dpws.TransportBinding;
import org.somda.sdc.dpws.TransportBindingException;
import org.somda.sdc.dpws.factory.TransportBindingFactory;
import org.somda.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.somda.sdc.dpws.http.grizzly.GrizzlyHttpServerRegistry;
import org.somda.sdc.dpws.soap.SoapMarshalling;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.factory.EnvelopeFactory;
import org.somda.sdc.dpws.soap.factory.SoapMessageFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import test.org.somda.common.LoggingTestWatcher;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

@ExtendWith(LoggingTestWatcher.class)
public class GrizzlyHttpServerRegistryIT extends DpwsTest {
    private GrizzlyHttpServerRegistry httpServerRegistry;
    private TransportBindingFactory transportBindingFactory;
    private SoapMessageFactory soapMessageFactory;
    private EnvelopeFactory envelopeFactory;

    @BeforeEach
    public void setUp() throws Exception {
        var override = new DefaultDpwsConfigModule() {
            @Override
            public void customConfigure() {
                bind(DpwsConfig.HTTP_GZIP_COMPRESSION, Boolean.class, true);
                bind(DpwsConfig.HTTP_RESPONSE_COMPRESSION_MIN_SIZE, Integer.class, 32);
            }
        };
        this.overrideBindings(override);
        super.setUp();
        httpServerRegistry = getInjector().getInstance(GrizzlyHttpServerRegistry.class);
        transportBindingFactory = getInjector().getInstance(TransportBindingFactory.class);
        soapMessageFactory = getInjector().getInstance(SoapMessageFactory.class);
        envelopeFactory = getInjector().getInstance(EnvelopeFactory.class);
        getInjector().getInstance(SoapMarshalling.class).startAsync().awaitRunning();
    }

    @Test
    public void registerContext() throws Exception {
        final URI baseUri = URI.create("http://127.0.0.1:0");
        final String ctxtPath = "/test/mulitple/path/segments";
        final AtomicBoolean isRequested = new AtomicBoolean(false);

        httpServerRegistry.startAsync().awaitRunning();
        URI srvUri = httpServerRegistry.registerContext(baseUri, ctxtPath, (req, res, ti) -> isRequested.set(true));

        TransportBinding httpBinding = transportBindingFactory.createHttpBinding(srvUri);
        httpBinding.onRequestResponse(createASoapMessage());
        assertThat(isRequested.get(), is(true));
        httpServerRegistry.stopAsync().awaitTerminated();
    }

    @Test
    public void unregisterContext() throws Exception {
        final String ctxtPath = "/test";
        final URI baseUri = URI.create("http://127.0.0.1:0");
        final AtomicBoolean isRequested = new AtomicBoolean(false);

        httpServerRegistry.startAsync().awaitRunning();
        URI srvUri = httpServerRegistry.registerContext(baseUri, ctxtPath, (req, res, ti) -> isRequested.set(true));

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
    public void registerMultipleContextsOnOneServer() throws MarshallingException, TransportException {
        URI baseUri = URI.create("http://127.0.0.1:0");
        final String ctxtPath1 = "/ctxt/path1";
        final String ctxtPath2 = "/ctxt/path2";

        final AtomicBoolean isPath1Requested = new AtomicBoolean(false);
        final AtomicBoolean isPath2Requested = new AtomicBoolean(false);

        httpServerRegistry.startAsync().awaitRunning();
        URI srvUri1 = httpServerRegistry.registerContext(baseUri, ctxtPath1, (req, res, ti) ->
                isPath1Requested.set(true));
        // uri1 has found a free port, attach uri 2 to the same
        baseUri = URI.create(String.format("%s:%s", srvUri1.getScheme(), srvUri1.getSchemeSpecificPart()));
        URI srvUri2 = httpServerRegistry.registerContext(baseUri, ctxtPath2, (req, res, ti) ->
                isPath2Requested.set(true));

        TransportBinding httpBinding1 = transportBindingFactory.createHttpBinding(srvUri1);
        TransportBinding httpBinding2 = transportBindingFactory.createHttpBinding(srvUri2);
        httpBinding1.onNotification(createASoapMessage());
        httpBinding2.onNotification(createASoapMessage());
        assertThat(isPath1Requested.get(), is(true));
        assertThat(isPath2Requested.get(), is(true));

        httpServerRegistry.unregisterContext(srvUri1, srvUri1.getPath());

        isPath1Requested.set(false);
        try {
            httpBinding1.onNotification(createASoapMessage());
            fail();
        } catch (TransportBindingException e) {
        }

        assertThat(isPath1Requested.get(), is(false));

//        TODO: Re-enable this section!
//        // verify path 2 is still working after removing path 1
//        isPath2Requested.set(false);
//        assertFalse(isPath2Requested.get());
//        try {
//            httpBinding2.onRequestResponse(createASoapMessage());
//        } catch (SoapFaultException e) {
//            fail(e);
//        }
//        assertTrue(isPath2Requested.get());

        httpServerRegistry.stopAsync().awaitTerminated();
    }

    @Test
    public void registerMultipleServers() {
        final String localhostUri = "http://127.0.0.1:0";
        URI firstServer = httpServerRegistry.initHttpServer(URI.create(localhostUri));
        URI secondServer = httpServerRegistry.initHttpServer(URI.create(localhostUri));
        assertThat(firstServer.getPort(), is(not(secondServer.getPort())));
    }

    @Test
    public void defaultPort() {
        // Plausibility test that default port is assigned
        // Plus: there is always one listener expected that holds the port
        // If Grizzly is about to change this behavior, this test will fail
        HttpServer httpServer = GrizzlyHttpServerFactory.createHttpServer(URI.create("http://127.0.0.1:0"), true);
        assertTrue(httpServer.isStarted());
        assertEquals(1, httpServer.getListeners().size());
        assertThat(Iterables.get(httpServer.getListeners(), 0).getPort(), is(not(0)));
        httpServer.shutdown();
    }

    @Test
    public void gzipCompression() throws IOException {
        URI baseUri = URI.create("http://127.0.0.1:0");
        final String compressedPath = "/ctxt/path1";

        final String expectedString = "The quick brown fox jumps over the lazy dog";
        final String expectedResponse = "ABCEDFG12345";
        AtomicReference<String> resultString = new AtomicReference<>();

        httpServerRegistry.startAsync().awaitRunning();
        URI srvUri1 = httpServerRegistry.registerContext(
                baseUri, compressedPath, (req, res, ti) -> {
                    try {
                        // request should be decompressed transparently
                        byte[] bytes = req.readAllBytes();
                        resultString.set(new String(bytes));

                        // write response, which should be compressed transparently
                        res.write(expectedResponse.getBytes());
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
        assertNotEquals(expectedResponse.getBytes(), responseBytes);

        ByteArrayInputStream responseBais = new ByteArrayInputStream(responseBytes);
        byte[] decompressedResponseBytes;
        try (GZIPInputStream gzos = new GZIPInputStream(responseBais)) {
            decompressedResponseBytes = gzos.readAllBytes();
        }

        // decompressed response matches
        assertEquals(expectedResponse, new String(decompressedResponseBytes));

        // request was properly processed and decompressed in server
        assertEquals(expectedString, resultString.get());
    }

    private SoapMessage createASoapMessage() {
        return soapMessageFactory.createSoapMessage(envelopeFactory.createEnvelope());
    }
}