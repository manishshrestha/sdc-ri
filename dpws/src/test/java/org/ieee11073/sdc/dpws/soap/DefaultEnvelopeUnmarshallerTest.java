package org.ieee11073.sdc.dpws.soap;

import org.ieee11073.sdc.dpws.DpwsTest;
import org.ieee11073.sdc.dpws.soap.model.Envelope;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class DefaultEnvelopeUnmarshallerTest extends DpwsTest {
    @Override
    @Before
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