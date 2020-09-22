package org.somda.sdc.dpws;

import jregex.Matcher;
import jregex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import test.org.somda.common.LoggingTestWatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(LoggingTestWatcher.class)
public class RegexTest {

    private static final Pattern AUTHORITY_PATTERN = new Pattern("^" + DpwsConstants.AUTHORITY + "$");
    private static final Pattern URI_PATTERN = new Pattern(DpwsConstants.URI_REGEX);
    private static final Pattern SEGMENT_PATTERN = new Pattern(DpwsConstants.AUTHORITY);
    private static final Pattern URI_REFERENCE_PATTERN = new Pattern(DpwsConstants.URI_REFERENCE);
    private static final Pattern RELATIVE_URI_PATTERN = new Pattern(DpwsConstants.RELATIVE_URI);
    private static final Pattern ABSOLUTE_URI_PATTERN = new Pattern(DpwsConstants.ABSOLUTE_URI);

    @Test
    void segment() {
        {
            Matcher matcher = SEGMENT_PATTERN.matcher("a%2A-&+");
            assertTrue(matcher.matches());
        }
    }

    @Test
    void authority() {
        {
            Matcher matcher = AUTHORITY_PATTERN.matcher("@host:");
            assertTrue(matcher.matches());
            assertEquals(matcher.group("userInfo"), "");
            assertEquals(matcher.group("port"), "");
            assertEquals(matcher.group("host"), "host");
        }
        {
            Matcher matcher = AUTHORITY_PATTERN.matcher("user@host:123");
            assertTrue(matcher.matches());
            assertEquals(matcher.group("userInfo"), "user");
            assertEquals(matcher.group("port"), "123");
            assertEquals(matcher.group("host"), "host");
        }
        {
            Matcher matcher = AUTHORITY_PATTERN.matcher("user@[10೬0:0:0:0:8:800:200C:417A]:123");
            assertFalse(matcher.matches());
        }
        {
            Matcher matcher = AUTHORITY_PATTERN.matcher("user@[1060:0:0:0:8:800:200C:417A]:123");
            assertTrue(matcher.matches());
            assertEquals(matcher.group("userInfo"), "user");
            assertEquals(matcher.group("port"), "123");
            assertEquals(matcher.group("host"), "[1060:0:0:0:8:800:200C:417A]");
        }
        {
            Matcher matcher = AUTHORITY_PATTERN.matcher("user@%C3%A4:123");
            assertTrue(matcher.matches());
            assertEquals(matcher.group("userInfo"), "user");
            assertEquals(matcher.group("port"), "123");
            assertEquals(matcher.group("host"), "%C3%A4");
        }
        {
            Matcher matcher = AUTHORITY_PATTERN.matcher("user@@:123");
            assertFalse(matcher.matches());
        }
    }

    @Test
    void uri() {
        {
            Matcher matcher = URI_PATTERN.matcher("scheme://@@");
            matcher.matches();
            assertFalse(matcher.matches());
        }
        {
            Matcher matcher = URI_PATTERN.matcher("scheme://user@%C3%A4:123/path?query?query#fragment");
            assertTrue(matcher.matches());
            assertEquals("user", matcher.group("userInfo"));
            assertEquals("123", matcher.group("port"));
            assertEquals("%C3%A4", matcher.group("host"));
            assertEquals("/path", matcher.group("path"));
            assertEquals("query?query", matcher.group("query"));
            assertEquals("fragment", matcher.group("fragment"));
        }
        {
            Matcher matcher = URI_PATTERN.matcher("scheme://user@%C3%A4:123/path?#?query#fragment");
            assertFalse(matcher.matches());
        }
        {
            Matcher matcher = URI_PATTERN.matcher("urn:example:animal:ferret:nose");
            assertTrue(matcher.matches());
            assertEquals("example:animal:ferret:nose", matcher.group("path"));
        }
        {
            Matcher matcher = URI_PATTERN.matcher("urn:%2A-");
            assertTrue(matcher.matches());
            assertEquals("%2A-", matcher.group("path"));
        }
    }

    @Test
    void uriReference() {
        {
            Matcher matcher = URI_REFERENCE_PATTERN.matcher("scheme://user@%C3%A4:123/path?query?query#fragment");
            assertTrue(matcher.matches());
            assertEquals("scheme://user@%C3%A4:123/path?query?query", matcher.group("absoluteUri"));
            assertEquals("user", matcher.group("userInfo"));
            assertEquals("123", matcher.group("port"));
            assertEquals("%C3%A4", matcher.group("host"));
            assertEquals("/path", matcher.group("path"));
            assertEquals("query?query", matcher.group("absoluteUriQuery"));
            assertEquals("fragment", matcher.group("fragment"));
            assertEquals("//user@%C3%A4:123/path?query?query", matcher.group("hierPart"));
            assertNull(matcher.group("relativeUri"));
            assertNull(matcher.group("opaquePart"));
        }
        {
            Matcher matcher = URI_REFERENCE_PATTERN.matcher("scheme:aaahhh??");
            assertTrue(matcher.matches());
            assertEquals("scheme:aaahhh??", matcher.group("absoluteUri"));
            assertEquals("aaahhh??", matcher.group("opaquePart"));
            assertNull(matcher.group("relativeUri"));
            assertNull(matcher.group("hierPart"));
        }
        {
            Matcher matcher = URI_REFERENCE_PATTERN.matcher("/path?query?query#fragment");
            assertTrue(matcher.matches());
            assertEquals("/path?query?query", matcher.group("relativeUri"));
            assertEquals("/path", matcher.group("path"));
            assertEquals("query?query", matcher.group("relativeUriQuery"));
            assertEquals("fragment", matcher.group("fragment"));
            assertNull(matcher.group("absoluteUri"));
            assertNull(matcher.group("netPath"));
            assertNull(matcher.group("relPath"));
        }
        {
            Matcher matcher = URI_REFERENCE_PATTERN.matcher("//user@%C3%A4:123/path?query?query#fragment");
            assertTrue(matcher.matches());
            assertEquals("//user@%C3%A4:123/path?query?query", matcher.group("relativeUri"));
            assertEquals("user", matcher.group("userInfo"));
            assertEquals("123", matcher.group("port"));
            assertEquals("%C3%A4", matcher.group("host"));
            assertEquals("/path", matcher.group("path"));
            assertEquals("query?query", matcher.group("relativeUriQuery"));
            assertEquals("fragment", matcher.group("fragment"));
            assertNull(matcher.group("absoluteUri"));
            assertNull(matcher.group("relPath"));
        }
    }

    @Test
    void absoluteUri() {
        {
            Matcher matcher = ABSOLUTE_URI_PATTERN.matcher("scheme://user@%C3%A4:123/path?query?query");
            assertTrue(matcher.matches());
            assertEquals("scheme://user@%C3%A4:123/path?query?query", matcher.group("absoluteUri"));
            assertEquals("user", matcher.group("userInfo"));
            assertEquals("123", matcher.group("port"));
            assertEquals("%C3%A4", matcher.group("host"));
            assertEquals("/path", matcher.group("path"));
            assertEquals("query?query", matcher.group("absoluteUriQuery"));
            assertNull(matcher.group("opaquePart"));
        }
        {
            Matcher matcher = ABSOLUTE_URI_PATTERN.matcher("scheme:aaahhh??");
            assertTrue(matcher.matches());
            assertEquals("scheme:aaahhh??", matcher.group("absoluteUri"));
            assertEquals("aaahhh??", matcher.group("opaquePart"));
            assertNull(matcher.group("hierPart"));
        }
    }

    @Test
    void relativeUri() {
        {
            Matcher matcher = RELATIVE_URI_PATTERN.matcher("/path?query?query");
            assertTrue(matcher.matches());
            assertEquals("/path?query?query", matcher.group("relativeUri"));
            assertEquals("/path", matcher.group("path"));
            assertEquals("query?query", matcher.group("relativeUriQuery"));
            assertNull(matcher.group("authority"));
            assertNull(matcher.group("relPath"));
        }
        {
            Matcher matcher = RELATIVE_URI_PATTERN.matcher("//user@%C3%A4:123/path?query?query");
            assertTrue(matcher.matches());
            assertEquals("//user@%C3%A4:123/path?query?query", matcher.group("relativeUri"));
            assertEquals("user", matcher.group("userInfo"));
            assertEquals("123", matcher.group("port"));
            assertEquals("%C3%A4", matcher.group("host"));
            assertEquals("/path", matcher.group("path"));
            assertEquals("query?query", matcher.group("relativeUriQuery"));
            assertNull(matcher.group("relPath"));
        }
        {
            Matcher matcher = RELATIVE_URI_PATTERN.matcher("*");
            assertTrue(matcher.matches());
            assertEquals("*", matcher.group("relativeUri"));
            assertEquals("*", matcher.group("relPath"));
            assertNull(matcher.group("path"));
            assertNull(matcher.group("authority"));
        }
    }
}
