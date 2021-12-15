package org.somda.sdc.dpws.soap.wsdiscovery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.somda.sdc.dpws.DpwsTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void isScopesMatchingRfc3986Test() {
        var superset = List.of("http://a.de/abc/d");
        // equals
        assertTrue(doesMatch(superset, "http://a.de/abc/d"));
        // case insensitive schema
        assertTrue(doesMatch(superset, "HTTP://a.de/abc/d"));
        // case insensitive authority
        assertTrue(doesMatch(superset, "http://A.dE/abc/d"));
        // match by segment
        assertTrue(doesMatch(superset, "http://a.de/abc"));
        // match by segment, trailing slash ignored
        assertTrue(doesMatch(superset, "http://a.de/abc/"));
        // match if subset has no segments
        assertTrue(doesMatch(superset, "http://a.de"));
        // query parameters are ignored during comparison
        assertTrue(doesMatch(superset, "http://a.de/abc?a=x&b=y"));

        // case sensitive segment
        assertFalse(doesMatch(superset, "http://a.de/Abc/d"));
        // encoded URI doesn't match even if it would after decoding (http://a.de/abc/d)
        assertFalse(doesMatch(superset, "http%3A%2F%2Fa.de%2Fabc%2Fd"));
        // doesn't match if only part of segment matches
        assertFalse(doesMatch(superset, "http://a.de/ab"));
        // doesn't match if subset has more segment than superset
        assertFalse(doesMatch(superset, "http://a.de/abc/d/d"));
        // doesn't match if subset has '.' or '..' in path
        assertFalse(doesMatch(superset, "http://a.de/abc/./d"));
        assertFalse(doesMatch(superset, "http://a.de/abc/../d"));
        // doesn't match if superset and subset equals but contains '.' segment
        assertFalse(doesMatch(List.of("http://a.de/abc/./d"), "http://a.de/abc/./d"));
    }

    private boolean doesMatch(List<String> superset, String s) {
        return wsDiscoveryUtil.isScopesMatching(superset, List.of(s), MatchBy.RFC3986);
    }
}