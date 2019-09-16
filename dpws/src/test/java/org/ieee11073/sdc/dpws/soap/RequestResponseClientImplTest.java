package org.ieee11073.sdc.dpws.soap;

import org.ieee11073.sdc.dpws.DpwsTest;
import org.ieee11073.sdc.dpws.soap.factory.RequestResponseClientFactory;
import org.ieee11073.sdc.dpws.soap.model.Envelope;
import org.ieee11073.sdc.dpws.soap.interception.*;
import org.ieee11073.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.AttributedURIType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RequestResponseClientImplTest extends DpwsTest {
    private List<String> dispatchedSequence;
    
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        getInjector().getInstance(SoapMarshalling.class).startAsync().awaitRunning();
        dispatchedSequence = new ArrayList<>();
    }

    @Test
    public void sendRequestResponse() throws Exception {
        SoapMarshalling unmarshaller = getInjector().getInstance(SoapMarshalling.class);
        Envelope soapEnv = unmarshaller.unmarshal(getClass().getResourceAsStream("soap-envelope.xml"));

        SoapUtil soapUtil = getInjector().getInstance(SoapUtil.class);
        SoapMessage request = soapUtil.createMessage(soapEnv);
        final SoapMessage response = soapUtil.createMessage();
        AttributedURIType responseAction = getInjector().getInstance(WsAddressingUtil.class)
                .createAttributedURIType("http://response-action");
        response.getWsAddressingHeader().setAction(responseAction);

        RequestResponseClientFactory clientFactory = getInjector().getInstance(RequestResponseClientFactory.class);
        RequestResponseClient rrClient = clientFactory.createRequestResponseClient(req -> {
            dispatchedSequence.add("NETWORK");
            return response;
        });

        rrClient.register(new Interceptor() {
            @MessageInterceptor(direction = Direction.REQUEST)
            InterceptorResult onDelete(RequestObject rInfo) {
                dispatchedSequence.add("REQUEST(MAX)");
                return InterceptorResult.PROCEED;
            }
        });

        rrClient.register(new Interceptor() {
            @MessageInterceptor(direction = Direction.REQUEST, sequenceNumber = 5)
            InterceptorResult onDelete(RequestObject rInfo) {
                dispatchedSequence.add("REQUEST(5)");
                return InterceptorResult.PROCEED;
            }
        });

        rrClient.register(new Interceptor() {
            @MessageInterceptor(value = "http://example.com/fabrikam/mail/Delete", direction = Direction.REQUEST)
            InterceptorResult onDelete(RequestObject rInfo) {
                dispatchedSequence.add("REQUEST(ACTION, MAX)");
                return InterceptorResult.PROCEED;
            }
        });

        // Shall be skipped since argument is missing
        rrClient.register(new Interceptor() {
            @MessageInterceptor(value = "http://example.com/fabrikam/mail/Delete", direction = Direction.REQUEST)
            InterceptorResult onDelete() {
                dispatchedSequence.add("INVALID REQUEST(ACTION, MAX)");
                return InterceptorResult.PROCEED;
            }
        });

        // Shall be skipped since argument is RequestResponse, but should be Request
        rrClient.register(new Interceptor() {
            @MessageInterceptor(value = "http://example.com/fabrikam/mail/Delete", direction = Direction.REQUEST)
            InterceptorResult onDelete(RequestResponseObject rrInfo) {
                dispatchedSequence.add("INVALID REQUEST(ACTION, MAX)");
                return InterceptorResult.PROCEED;
            }
        });

        rrClient.register(new Interceptor() {
            @MessageInterceptor(direction = Direction.RESPONSE)
            InterceptorResult onDelete(RequestResponseObject rrInfo) {
                dispatchedSequence.add("RESPONSE(MAX)");
                return InterceptorResult.PROCEED;
            }
        });

        // Shall be skipped since response action is not "http://example.com/fabrikam/mail/Delete"
        rrClient.register(new Interceptor() {
            @MessageInterceptor(value = "http://example.com/fabrikam/mail/Delete", direction = Direction.RESPONSE)
            InterceptorResult onDelete(RequestResponseObject rrInfo) {
                dispatchedSequence.add("INVALID RESPONSE(ACTION, MAX)");
                return InterceptorResult.PROCEED;
            }
        });

        rrClient.register(new Interceptor() {
            @MessageInterceptor(value = "http://response-action", direction = Direction.RESPONSE)
            InterceptorResult onDelete(RequestResponseObject rrInfo) {
                dispatchedSequence.add("RESPONSE(ACTION,MAX)");
                return InterceptorResult.PROCEED;
            }
        });

        assertSame(response, rrClient.sendRequestResponse(request));

        assertEquals(6, dispatchedSequence.size());
        assertEquals("REQUEST(5)", dispatchedSequence.get(0));
        assertEquals("REQUEST(MAX)", dispatchedSequence.get(1));
        assertEquals("REQUEST(ACTION, MAX)", dispatchedSequence.get(2));
        assertEquals("NETWORK", dispatchedSequence.get(3));
        assertEquals("RESPONSE(MAX)", dispatchedSequence.get(4));
        assertEquals("RESPONSE(ACTION,MAX)", dispatchedSequence.get(5));
    }
}