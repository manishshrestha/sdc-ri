package it.org.ieee11073.sdc.dpws.http.grizzly;

import com.google.common.collect.Iterables;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.ieee11073.sdc.dpws.DpwsTest;
import org.ieee11073.sdc.dpws.TransportBinding;
import org.ieee11073.sdc.dpws.TransportBindingException;
import org.ieee11073.sdc.dpws.factory.TransportBindingFactory;
import org.ieee11073.sdc.dpws.http.grizzly.GrizzlyHttpServerRegistry;
import org.ieee11073.sdc.dpws.soap.SoapMarshalling;
import org.ieee11073.sdc.dpws.soap.SoapMessage;
import org.ieee11073.sdc.dpws.soap.exception.MarshallingException;
import org.ieee11073.sdc.dpws.soap.exception.TransportException;
import org.ieee11073.sdc.dpws.soap.factory.EnvelopeFactory;
import org.ieee11073.sdc.dpws.soap.factory.SoapMessageFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import test.org.ieee11073.common.LoggingTestWatcher;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

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
        final URI baseUri = URI.create("http://127.0.0.1:0");
        final String ctxtPath1 = "/path1";
        final String ctxtPath2 = "/path2";

        final AtomicBoolean isPath1Requested = new AtomicBoolean(false);
        final AtomicBoolean isPath2Requested = new AtomicBoolean(false);

        httpServerRegistry.startAsync().awaitRunning();
        URI srvUri1 = httpServerRegistry.registerContext(baseUri, ctxtPath1, (req, res, ti) ->
                isPath1Requested.set(true));
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

    private SoapMessage createASoapMessage() {
        return soapMessageFactory.createSoapMessage(envelopeFactory.createEnvelope());
    }
}