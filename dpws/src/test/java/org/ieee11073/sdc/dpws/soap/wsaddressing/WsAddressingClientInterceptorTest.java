package org.ieee11073.sdc.dpws.soap.wsaddressing;

import org.ieee11073.sdc.dpws.DpwsTest;
import org.ieee11073.sdc.dpws.soap.SoapMessage;
import org.ieee11073.sdc.dpws.soap.RequestResponseClient;
import org.ieee11073.sdc.dpws.soap.factory.SoapMessageFactory;
import org.ieee11073.sdc.dpws.soap.factory.EnvelopeFactory;
import org.ieee11073.sdc.dpws.soap.factory.RequestResponseClientFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class WsAddressingClientInterceptorTest extends DpwsTest {

    private RequestResponseClient client;
    private EnvelopeFactory envelopeFactory;
    private SoapMessageFactory soapMessageFactory;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        envelopeFactory = getInjector().getInstance(EnvelopeFactory.class);
        soapMessageFactory = getInjector().getInstance(SoapMessageFactory.class);
        RequestResponseClientFactory requestResponseClientFactory = getInjector()
                .getInstance(RequestResponseClientFactory.class);

        client = requestResponseClientFactory.createRequestResponseClient(request -> {
            SoapMessage response = soapMessageFactory.createSoapMessage(
                    envelopeFactory.createEnvelope("http://response", null));
            request.getWsAddressingHeader().getMessageId().ifPresent(attributedURIType ->
                    response.getWsAddressingHeader().setMessageId(attributedURIType));
            return response;
        });
        client.register(getInjector().getInstance(WsAddressingClientInterceptor.class));
    }

    @Test
    public void processMessage() throws Exception {
        SoapMessage req = soapMessageFactory.createSoapMessage(envelopeFactory
                .createEnvelope("http://request", null));
        SoapMessage res = client.sendRequestResponse(req);
        Assert.assertTrue(res.getWsAddressingHeader().getMessageId().isPresent());
    }
}