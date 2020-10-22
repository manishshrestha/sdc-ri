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
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DiscoveredDeviceResolverTest extends DpwsTest {
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
    public void resolve() throws Exception {
        var expectedUri = "http://expectedUri";
        List<String> xAddrsInHello = Arrays.asList("http://inHello1", "http://inHello2");
        EndpointReferenceType epr = wsaUtil.createEprWithAddress(expectedUri);
        HelloType hType = objFactory.createHelloType();
        HelloMessage hMsg = new HelloMessage(hType);
        hType.setEndpointReference(epr);
        hType.setXAddrs(xAddrsInHello);
        hType.setScopes(objFactory.createScopesType());

        ResolveMatchType resolveMatchType = objFactory.createResolveMatchType();
        resolveMatchType.setScopes(objFactory.createScopesType());
        List<String> xAddrsInResolveMatches = Arrays.asList("http://inResolveMatches1", "http://inResolveMatches2");
        resolveMatchType.setEndpointReference(epr);
        resolveMatchType.setXAddrs(xAddrsInResolveMatches);
        ResolveMatchesType rmType = objFactory.createResolveMatchesType();
        rmType.setResolveMatch(resolveMatchType);

        WsDiscoveryClient wsdClient = mock(WsDiscoveryClient.class);
        when(wsdClient.sendResolve(epr)).thenReturn(Futures.immediateFuture(rmType));

        DiscoveredDeviceResolver dpr = new DiscoveredDeviceResolver(wsdClient, Duration.ofSeconds(1), true, wsaUtil, "abcd");

        Collection<DiscoveredDevice> actualWithResolveMatches = dpr.resolve(hMsg);
        assertEquals(1, actualWithResolveMatches.size());
        assertEquals(expectedUri, actualWithResolveMatches.stream().findFirst().get().getEprAddress());
        assertEquals(xAddrsInHello, actualWithResolveMatches.stream().findFirst().get().getXAddrs());

        hType.setXAddrs(new ArrayList<>());
        Collection<DiscoveredDevice> actualWithoutResolveMatches = dpr.resolve(hMsg);
        assertEquals(1, actualWithoutResolveMatches.size());
        assertEquals(expectedUri, actualWithoutResolveMatches.stream().findFirst().get().getEprAddress());
        assertEquals(xAddrsInResolveMatches, actualWithoutResolveMatches.stream().findFirst().get().getXAddrs());
    }
}