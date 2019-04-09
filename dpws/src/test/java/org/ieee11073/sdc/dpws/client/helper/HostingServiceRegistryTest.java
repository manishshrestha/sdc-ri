package org.ieee11073.sdc.dpws.client.helper;

import org.ieee11073.sdc.dpws.service.WritableHostingServiceProxy;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.junit.Assert.*;

public class HostingServiceRegistryTest {

    private URI expectedUri;
    private WritableHostingServiceProxy hsp1;
    private WritableHostingServiceProxy hsp2;
    private HostingServiceRegistry hsr;

    @Before
    public void setUp() throws Exception {
        expectedUri = URI.create("http://test");

        hsp1 = mock(WritableHostingServiceProxy.class);
        when(hsp1.getEndpointReferenceAddress()).thenReturn(expectedUri);

        hsp2 = mock(WritableHostingServiceProxy.class);
        when(hsp2.getEndpointReferenceAddress()).thenReturn(expectedUri);

        hsr = new HostingServiceRegistry();
    }

    @Test
    public void registerOrUpdate() throws Exception {
        assertFalse(hsr.get(expectedUri).isPresent());

        hsr.registerOrUpdate(hsp1);
        assertTrue(hsr.get(expectedUri).isPresent());

        assertEquals(hsp1, hsr.registerOrUpdate(hsp2));
    }

    @Test
    public void unregister() throws Exception {
        assertFalse(hsr.unregister(expectedUri).isPresent());

        hsr.registerOrUpdate(hsp1);
        assertTrue(hsr.get(expectedUri).isPresent());

        assertTrue(hsr.unregister(expectedUri).isPresent());

        assertFalse(hsr.get(expectedUri).isPresent());
    }

}