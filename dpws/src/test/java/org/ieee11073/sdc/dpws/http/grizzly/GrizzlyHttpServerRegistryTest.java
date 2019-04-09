package org.ieee11073.sdc.dpws.http.grizzly;

import org.ieee11073.sdc.dpws.DpwsTest;
import org.ieee11073.sdc.dpws.TransportBinding;
import org.ieee11073.sdc.dpws.factory.TransportBindingFactory;
import org.ieee11073.sdc.dpws.soap.SoapMarshalling;
import org.ieee11073.sdc.dpws.soap.SoapMessage;
import org.ieee11073.sdc.dpws.soap.SoapUtil;
import org.ieee11073.sdc.dpws.soap.exception.TransportException;
import org.ieee11073.sdc.dpws.soap.factory.EnvelopeFactory;
import org.ieee11073.sdc.dpws.soap.factory.SoapMessageFactory;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.AttributedURIType;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;

public class GrizzlyHttpServerRegistryTest extends DpwsTest {
    private final static String HOST = "localhost";
    private final static Integer PORT = 9999;
    private GrizzlyHttpServerRegistry srvReg;
    private TransportBindingFactory tbFactory;
    private SoapMessageFactory smf;
    private EnvelopeFactory ef;
    private SoapUtil soapUtil;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        srvReg = getInjector().getInstance(GrizzlyHttpServerRegistry.class);
        tbFactory = getInjector().getInstance(TransportBindingFactory.class);
        smf = getInjector().getInstance(SoapMessageFactory.class);
        ef = getInjector().getInstance(EnvelopeFactory.class);
        soapUtil = getInjector().getInstance(SoapUtil.class);
        getInjector().getInstance(SoapMarshalling.class).startAsync().awaitRunning();
    }

    @Test
    public void registerContext() throws Exception {
        String expectedActionUri = "http://test-uri";
        String ctxtPath = "/test";

        srvReg.startAsync().awaitRunning();
        URI srvUri = srvReg.registerContext(HOST, PORT, ctxtPath, (requestMessage, responseMessage, ti) -> {
            try {
                int next = requestMessage.read();
                while (next != -1) {
                    responseMessage.write(next);
                    next = requestMessage.read();
                }
                requestMessage.close();
                responseMessage.close();
            } catch (IOException e) {
                throw new TransportException(e);
            }
        });

        SoapMessage request = smf.createSoapMessage(ef.createEnvelope());
        soapUtil.setWsaAction(request, expectedActionUri);
        TransportBinding httpBinding = tbFactory.createHttpBinding(srvUri);
        SoapMessage response = httpBinding.onRequestResponse(request);
        Optional<AttributedURIType> actualAction = response.getWsAddressingHeader().getAction();
        assertThat(actualAction, is(not(Optional.empty())));
        String actualActionUri = actualAction.get().getValue();
        assertThat(actualActionUri, is(expectedActionUri));
        srvReg.stopAsync().awaitTerminated();
    }

    @Test
    public void unregisterContext() throws Exception {
        String ctxtPath = "/test";

        AtomicBoolean isRequested = new AtomicBoolean(false);

        srvReg.startAsync().awaitRunning();
        URI srvUri = srvReg.registerContext(HOST, PORT, ctxtPath, (requestMessage, responseMessage, ti) -> {
            try {
                requestMessage.close();
                responseMessage.close();
            } catch (IOException e) {
                throw new TransportException(e);
            }
            isRequested.set(true);
        });

        SoapMessage request = smf.createSoapMessage(ef.createEnvelope());
        TransportBinding httpBinding = tbFactory.createHttpBinding(srvUri);
        httpBinding.onNotification(request);
        assertThat(isRequested.get(), is(true));

        isRequested.set(false);
        srvReg.unregisterContext(HOST, PORT, ctxtPath);
        try {
            httpBinding.onNotification(request);
            assertFalse(true);
        } catch (Exception e) {
            assertTrue(true);
        }
        assertThat(isRequested.get(), is(false));

        srvReg.stopAsync().awaitTerminated();
    }

}