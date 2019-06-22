package org.ieee11073.sdc.dpws.device.helper;

import org.junit.Test;

import java.net.URI;

import static junit.framework.TestCase.assertEquals;

public class UriBaseContextPathTest {
    final String expectedUrlPath = "/context/path";
    final String expectedUrnUuidSpecificPart = "uuid:550e8400-e29b-11d4-a716-446655440000";
    final String expectedUrnOidSpecificPart = "oid:1.3.6.1.4.1";

    final URI testUrl = URI.create("http://www.examp.le" + expectedUrlPath);
    final URI testUuid = URI.create("urn:" + expectedUrnUuidSpecificPart);
    final URI testOid = URI.create("urn:" + expectedUrnOidSpecificPart);

    @Test
    public void uriParts() {
        assertEquals(expectedUrlPath, testUrl.getPath());
        assertEquals(expectedUrnUuidSpecificPart, testUuid.getSchemeSpecificPart());
        assertEquals(expectedUrnOidSpecificPart, testOid.getSchemeSpecificPart());
    }

    @Test
    public void basePathDerivation() {
        final String expectedUrlBasePath = expectedUrlPath.substring(1);
        final String expectedUuidBasePath = expectedUrnUuidSpecificPart.substring("uuid:".length());
        final String expectedOidBasePath = expectedUrnOidSpecificPart.substring("oid:".length());;

        assertEquals(expectedUrlBasePath, new UriBaseContextPath(testUrl).get());
        assertEquals(expectedUuidBasePath, new UriBaseContextPath(testUuid).get());
        assertEquals(expectedOidBasePath, new UriBaseContextPath(testOid).get());

        assertEquals("", new UriBaseContextPath(URI.create("urn:foo:bar:1234")).get());
    }
}
