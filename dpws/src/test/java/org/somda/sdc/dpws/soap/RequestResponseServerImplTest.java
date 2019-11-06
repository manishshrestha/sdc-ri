package org.ieee11073.sdc.dpws.soap;

import org.ieee11073.sdc.dpws.DpwsTest;
import org.ieee11073.sdc.dpws.soap.factory.SoapMessageFactory;
import org.ieee11073.sdc.dpws.soap.model.Envelope;
import org.ieee11073.sdc.dpws.soap.interception.*;
import org.ieee11073.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.AttributedURIType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RequestResponseServerImplTest extends DpwsTest {
    private List<String> dispatchedSequence;
    private TransportInfo mockTransportInfo;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        mockTransportInfo = new TransportInfo("mock.scheme", "localhost", 123);

        getInjector().getInstance(SoapMarshalling.class).startAsync().awaitRunning();
        dispatchedSequence = new ArrayList<>();
    }

    @Test
    public void receiveRequestResponse() throws Exception {
        SoapMarshalling unmarshaller = getInjector().getInstance(SoapMarshalling.class);
        RequestResponseServer rrServer = getInjector().getInstance(RequestResponseServer.class);
        Envelope soapEnv = unmarshaller.unmarshal(getClass().getResourceAsStream("soap-envelope.xml"));

        SoapMessageFactory soapMessageFactory = getInjector().getInstance(SoapMessageFactory.class);
        SoapMessage request = soapMessageFactory.createSoapMessage(soapEnv);
        SoapMessage response = soapMessageFactory.createSoapMessage(new Envelope());
        AttributedURIType responseAction = getInjector().getInstance(WsAddressingUtil.class)
                .createAttributedURIType("http://response-action");
        response.getWsAddressingHeader().setAction(responseAction);

        rrServer.register(new Interceptor() {
            @MessageInterceptor(direction = Direction.REQUEST)
            void onDelete(RequestResponseObject rrInfo) {
                dispatchedSequence.add("REQUEST(MAX)");
            }
        });

        rrServer.register(new Interceptor() {
            @MessageInterceptor(direction = Direction.REQUEST, sequenceNumber = 5)
            void onDelete(RequestResponseObject rrInfo) {
                dispatchedSequence.add("REQUEST(5)");
            }
        });

        rrServer.register(new Interceptor() {
            @MessageInterceptor(value = "http://example.com/fabrikam/mail/Delete", direction = Direction.REQUEST)
            void onDelete(RequestResponseObject rrInfo) {
                dispatchedSequence.add("REQUEST(ACTION, MAX)");
            }
        });

        // Shall be skipped since argument is missing
        rrServer.register(new Interceptor() {
            @MessageInterceptor(value = "http://example.com/fabrikam/mail/Delete", direction = Direction.REQUEST)
            void onDelete() {
                dispatchedSequence.add("INVALID REQUEST(ACTION, MAX)");
            }
        });

        rrServer.register(new Interceptor() {
            @MessageInterceptor(direction = Direction.RESPONSE)
            void onDelete(RequestResponseObject rrInfo) {
                dispatchedSequence.add("RESPONSE(MAX)");
            }
        });

        // Shall be skipped since response action is not "http://example.com/fabrikam/mail/Delete"
        rrServer.register(new Interceptor() {
            @MessageInterceptor(value = "http://example.com/fabrikam/mail/Delete", direction = Direction.RESPONSE)
            void onDelete(RequestResponseObject rrInfo) {
                dispatchedSequence.add("INVALID RESPONSE(ACTION, MAX)");
            }
        });

        rrServer.register(new Interceptor() {
            @MessageInterceptor(value = "http://response-action" , direction = Direction.RESPONSE)
            void onDelete(RequestResponseObject rrInfo) {
                dispatchedSequence.add("RESPONSE(ACTION,MAX)");
            }
        });

        rrServer.receiveRequestResponse(request, response, mockTransportInfo);

        assertEquals(5, dispatchedSequence.size());
        assertEquals("REQUEST(5)", dispatchedSequence.get(0));
        assertEquals("REQUEST(MAX)", dispatchedSequence.get(1));
        assertEquals("REQUEST(ACTION, MAX)", dispatchedSequence.get(2));
        assertEquals("RESPONSE(MAX)", dispatchedSequence.get(3));
        assertEquals("RESPONSE(ACTION,MAX)", dispatchedSequence.get(4));
    }
}