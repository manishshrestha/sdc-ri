package org.ieee11073.sdc.dpws.soap.wsaddressing;

import org.ieee11073.sdc.dpws.DpwsTest;
import org.ieee11073.sdc.dpws.soap.RequestResponseServer;
import org.ieee11073.sdc.dpws.soap.SoapMarshalling;
import org.ieee11073.sdc.dpws.soap.SoapMessage;
import org.ieee11073.sdc.dpws.soap.TransportInfo;
import org.ieee11073.sdc.dpws.soap.exception.SoapFaultException;
import org.ieee11073.sdc.dpws.soap.factory.SoapMessageFactory;
import org.ieee11073.sdc.dpws.soap.interception.*;
import org.ieee11073.sdc.dpws.soap.model.Envelope;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.AttributedURIType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

public class WsAddressingServerInterceptorTest extends DpwsTest {

    private SoapMessage request;
    private SoapMessage response;
    private RequestResponseServer server;
    private TransportInfo mockTransportInfo;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        mockTransportInfo = new TransportInfo("http", "localhost", 123);

        InputStream soapStrm = getClass().getResourceAsStream("soap-envelope.xml");
        getInjector().getInstance(SoapMarshalling.class).startAsync().awaitRunning();
        Envelope soapEnv = getInjector().getInstance(SoapMarshalling.class).unmarshal(soapStrm);

        SoapMessageFactory soapMessageFactory = getInjector().getInstance(SoapMessageFactory.class);
        request = soapMessageFactory.createSoapMessage(soapEnv);
        response = soapMessageFactory.createSoapMessage(new Envelope());
        AttributedURIType responseAction = getInjector().getInstance(WsAddressingUtil.class)
                .createAttributedURIType("http://response-action");
        response.getWsAddressingHeader().setAction(responseAction);

        server = getInjector().getInstance(RequestResponseServer.class);
        server.register(new Interceptor() {
            @MessageInterceptor(value = "http://example.com/fabrikam/mail/Delete", direction = Direction.REQUEST)
            void dummyProcess(RequestResponseObject rrInfo) {
                // do nothing here
            }
        });
    }

    @Test
    public void testMessageIdDuplicationDetection() throws SoapFaultException {
        assertDoesNotThrow(() -> server.receiveRequestResponse(request, response, mockTransportInfo));
        assertThrows(SoapFaultException.class, () ->
                server.receiveRequestResponse(request, response, mockTransportInfo));
    }

    @Test
    public void testEmptyMessageIdException() {
        request.getWsAddressingHeader().setMessageId(null);

        try {
            server.receiveRequestResponse(request, response, mockTransportInfo);
            fail();
        } catch (SoapFaultException e) {
            assertEquals(WsAddressingConstants.MESSAGE_ADDRESSING_HEADER_REQUIRED,
                    e.getFault().getCode().getSubcode().getValue());
            assertEquals(1, e.getFault().getDetail().getAny().size());
            assertEquals(WsAddressingConstants.MESSAGE_ID.toString(), e.getFault().getDetail().getAny().get(0));
        }
    }
}