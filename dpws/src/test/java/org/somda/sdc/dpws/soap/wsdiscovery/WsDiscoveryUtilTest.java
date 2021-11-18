package org.somda.sdc.dpws.soap.wsdiscovery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.somda.sdc.dpws.DpwsTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WsDiscoveryUtilTest extends DpwsTest {
    private WsDiscoveryUtil wsDiscoveryUtil;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        this.wsDiscoveryUtil = getInjector().getInstance(WsDiscoveryUtil.class);
    }

    @Test
    void isScopesMatchingStrcmpRegressionTest() {
        var superset = List.of("http://a.de", "http://b.de");
        var subset = List.of("http://b.de");

        assertTrue(wsDiscoveryUtil.isScopesMatching(superset, subset, MatchBy.STRCMP0));
    }
}