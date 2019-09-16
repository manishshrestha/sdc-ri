package org.ieee11073.sdc.dpws.soap;

import org.ieee11073.sdc.dpws.DpwsTest;
import org.ieee11073.sdc.dpws.soap.model.Envelope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;


public class DefaultEnvelopeUnmarshallerTest extends DpwsTest {
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void testUnmarshal() throws Exception {
        SoapMarshalling unmarshaller = getInjector().getInstance(SoapMarshalling.class);
        unmarshaller.startAsync().awaitRunning();
        Envelope actualEnv = unmarshaller.unmarshal(getClass().getResourceAsStream("soap-envelope.xml"));
        assertNotNull(actualEnv);
    }
}