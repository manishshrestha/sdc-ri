package org.somda.sdc.dpws.client.helper;

import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.somda.sdc.dpws.DpwsTest;
import org.somda.sdc.dpws.client.DiscoveredDevice;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryClient;
import org.somda.sdc.dpws.soap.wsdiscovery.event.HelloMessage;
import org.somda.sdc.dpws.soap.wsdiscovery.model.HelloType;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ObjectFactory;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ResolveMatchType;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ResolveMatchesType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DiscoveredDeviceResolverTest extends DpwsTest {
    private WsAddressingUtil wsaUtil;
    private ObjectFactory objFactory;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        wsaUtil = getInjector().getInstance(WsAddressingUtil.class);
        objFactory = new ObjectFactory();
    }

    @Test
    void resolve() throws Exception {
        var expectedUri = "http://expectedUri";
        List<String> xAddrsInHello = Arrays.asList("http://inHello1", "http://inHello2");
        EndpointReferenceType epr = wsaUtil.createEprWithAddress(expectedUri);
        HelloType hType = HelloType.builder()
            .withEndpointReference(epr)
            .withXAddrs(xAddrsInHello)
            .withScopes(objFactory.createScopesType())
            .build();
        HelloMessage hMsg = new HelloMessage(hType);


        List<String> xAddrsInResolveMatches = Arrays.asList("http://inResolveMatches1", "http://inResolveMatches2");

        ResolveMatchType resolveMatchType = ResolveMatchType.builder()
            .withScopes(objFactory.createScopesType())
            .withEndpointReference(epr)
            .withXAddrs(xAddrsInResolveMatches)
            .build();
        ResolveMatchesType rmType = ResolveMatchesType.builder()
            .withResolveMatch(resolveMatchType).build();

        WsDiscoveryClient wsdClient = mock(WsDiscoveryClient.class);
        when(wsdClient.sendResolve(epr)).thenReturn(Futures.immediateFuture(rmType));

        DiscoveredDeviceResolver dpr = new DiscoveredDeviceResolver(wsdClient, Duration.ofSeconds(1), true, wsaUtil, "abcd");

        Optional<DiscoveredDevice> actualWithResolveMatches = dpr.resolve(hMsg);
        assertTrue(actualWithResolveMatches.isPresent());
        assertEquals(expectedUri, actualWithResolveMatches.get().getEprAddress());
        assertEquals(xAddrsInHello, actualWithResolveMatches.get().getXAddrs());

        hType = hType.newCopyBuilder().withXAddrs(new ArrayList<>()).build();
        hMsg = new HelloMessage(hType);

        Optional<DiscoveredDevice> actualWithoutResolveMatches = dpr.resolve(hMsg);
        assertTrue(actualWithoutResolveMatches.isPresent());
        assertEquals(expectedUri, actualWithoutResolveMatches.get().getEprAddress());
        assertEquals(xAddrsInResolveMatches, actualWithoutResolveMatches.get().getXAddrs());
    }
}