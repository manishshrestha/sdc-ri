package it.org.somda.sdc.dpws;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.dpws.DpwsTest;
import org.somda.sdc.dpws.TransportBinding;
import org.somda.sdc.dpws.factory.TransportBindingFactory;
import org.somda.sdc.dpws.guice.DefaultDpwsConfigModule;
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
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

@ExtendWith(LoggingTestWatcher.class)
public class ApacheHttpClientTransportBindingFactoryImplIT extends DpwsTest {

    private static final Logger LOG = LoggerFactory.getLogger(ApacheHttpClientTransportBindingFactoryImplIT.class);

    private TransportBindingFactory transportBindingFactory;
    private SoapMessageFactory soapMessageFactory;
    private EnvelopeFactory envelopeFactory;
    private SoapMarshalling marshalling;

    @BeforeEach
    public void setUp() throws Exception {
        var override = new DefaultDpwsConfigModule() {
            @Override
            public void customConfigure() {
                bind(DpwsConfig.HTTP_GZIP_COMPRESSION, Boolean.class, true);
            }
        };
        this.overrideBindings(override);
        super.setUp();
        transportBindingFactory = getInjector().getInstance(TransportBindingFactory.class);
        soapMessageFactory = getInjector().getInstance(SoapMessageFactory.class);
        envelopeFactory = getInjector().getInstance(EnvelopeFactory.class);
        marshalling = getInjector().getInstance(SoapMarshalling.class);
        marshalling.startAsync().awaitRunning();
    }

    @Test
    void testGzipCompression() throws Exception {
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
        GzipResponseHandler handler = new GzipResponseHandler(responseBytes);
        var inetSocketAddress = new InetSocketAddress(baseUri.getHost(), baseUri.getPort());
        var server = spawnHttpServer(inetSocketAddress, handler);

        // replace the port
        baseUri = new URI(
                baseUri.getScheme(),
                baseUri.getUserInfo(),
                baseUri.getHost(),
                server.getAddress().getPort(),
                baseUri.getPath(),
                baseUri.getQuery(),
                baseUri.getFragment());

        // make request to our server
        TransportBinding httpBinding1 = transportBindingFactory.createHttpBinding(baseUri);
        SoapMessage response = httpBinding1.onRequestResponse(createASoapMessage());

        ByteArrayOutputStream actualResponseStream = new ByteArrayOutputStream();
        marshalling.marshal(response.getEnvelopeWithMappedHeaders(), actualResponseStream);

        // response bytes should exactly match our expected bytes, transparently decompressed
        assertArrayEquals(expectedResponseStream.toByteArray(), actualResponseStream.toByteArray());
    }


    static class GzipResponseHandler implements HttpHandler {
        private final byte[] compressedResponse;

        GzipResponseHandler(byte[] response) throws IOException {
            ByteArrayOutputStream responseBais = new ByteArrayOutputStream();
            try (GZIPOutputStream gzos = new GZIPOutputStream(responseBais)) {
                gzos.write(response);
            }
            this.compressedResponse = responseBais.toByteArray();
        }

        @Override
        public void handle(HttpExchange t) throws IOException {

            List<String> strings = t.getRequestHeaders().get(HttpHeaders.ACCEPT_ENCODING);
            if (strings.stream().noneMatch(x -> x.contains("gzip"))) {
                LOG.error("No Accept-Encoding with value gzip in request header");
                throw new RuntimeException("No Accept-Encoding with value gzip in request header");
            }

            t.getResponseHeaders().add(HttpHeaders.CONTENT_ENCODING, "gzip");
            t.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, SoapConstants.MEDIA_TYPE_SOAP);
            t.sendResponseHeaders(200, compressedResponse.length);
            OutputStream os = t.getResponseBody();
            os.write(compressedResponse);
            os.close();
        }
    }

    HttpServer spawnHttpServer(InetSocketAddress addr, HttpHandler httpHandler) throws IOException {
        HttpServer server = HttpServer.create(addr, 0);
        server.createContext("/", httpHandler);
        server.setExecutor(null); // creates a default executor
        server.start();
        return server;
    }

    private SoapMessage createASoapMessage() {
        return soapMessageFactory.createSoapMessage(envelopeFactory.createEnvelope());
    }
}
