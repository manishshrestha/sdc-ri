package org.somda.sdc.dpws;

import jregex.Matcher;
import jregex.Pattern;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RFC2396RegexTest {
    private static final Pattern URI_REFERENCE_PATTERN = new Pattern(RFC2396Constants.URI_REFERENCE);
    private static final Pattern RELATIVE_URI_PATTERN = new Pattern(RFC2396Constants.RELATIVE_URI);
    private static final Pattern ABSOLUTE_URI_PATTERN = new Pattern(RFC2396Constants.ABSOLUTE_URI);

    @Test
    void uriReference() {
        {
            Matcher matcher = URI_REFERENCE_PATTERN.matcher("scheme://user@%C3%A4:123/path?query?query#fragment");
            assertTrue(matcher.matches());
            assertEquals("scheme://user@%C3%A4:123/path?query?query", matcher.group("absoluteUri"));
            assertEquals("user@%C3%A4:123", matcher.group("authority"));
            assertEquals("user@%C3%A4:123", matcher.group("regName"));
            assertEquals("/path", matcher.group("absPath"));
            assertEquals("query?query", matcher.group("absoluteUriQuery"));
            assertEquals("fragment", matcher.group("fragment"));
            assertEquals("//user@%C3%A4:123/path?query?query", matcher.group("hierPart"));
            assertNull(matcher.group("relativeUri"));
            assertNull(matcher.group("opaquePart"));
        }
        {
            Matcher matcher = URI_REFERENCE_PATTERN.matcher("scheme://user@host:123/path?query?query#fragment");
            assertTrue(matcher.matches());
            assertEquals("scheme://user@host:123/path?query?query", matcher.group("absoluteUri"));
            assertEquals("user", matcher.group("userInfo"));
            assertEquals("user@host:123", matcher.group("authority"));
            assertEquals("123", matcher.group("port"));
            assertEquals("host", matcher.group("host"));
            assertEquals("/path", matcher.group("absPath"));
            assertEquals("query?query", matcher.group("absoluteUriQuery"));
            assertEquals("fragment", matcher.group("fragment"));
            assertEquals("//user@host:123/path?query?query", matcher.group("hierPart"));
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
            assertEquals("/path", matcher.group("absPath"));
            assertEquals("query?query", matcher.group("relativeUriQuery"));
            assertEquals("fragment", matcher.group("fragment"));
            assertNull(matcher.group("absoluteUri"));
            assertNull(matcher.group("netPath"));
            assertNull(matcher.group("relPath"));
        }
        {
            Matcher matcher = URI_REFERENCE_PATTERN.matcher("//user@C3A4:123/path?query?query#fragment");
            assertTrue(matcher.matches());
            assertEquals("//user@C3A4:123/path?query?query", matcher.group("relativeUri"));
            assertEquals("user", matcher.group("userInfo"));
            assertEquals("123", matcher.group("port"));
            assertEquals("C3A4", matcher.group("host"));
            assertEquals("/path", matcher.group("absPath"));
            assertEquals("query?query", matcher.group("relativeUriQuery"));
            assertEquals("fragment", matcher.group("fragment"));
            assertNull(matcher.group("absoluteUri"));
            assertNull(matcher.group("relPath"));
        }
    }

    @Test
    void absoluteUri() {
        {
            Matcher matcher = ABSOLUTE_URI_PATTERN.matcher("scheme://user@C3A4:123/path?query?query");
            assertTrue(matcher.matches());
            assertEquals("scheme://user@C3A4:123/path?query?query", matcher.group("absoluteUri"));
            assertEquals("//user@C3A4:123/path?query?query", matcher.group("hierPart"));
            assertEquals("//user@C3A4:123/path", matcher.group("netPath"));
            assertEquals("user@C3A4:123", matcher.group("authority"));
            assertNull(matcher.group("regName"));
            assertEquals("C3A4:123", matcher.group("hostPort"));
            assertEquals("user", matcher.group("userInfo"));
            assertEquals("123", matcher.group("port"));
            assertEquals("C3A4", matcher.group("host"));
            assertEquals("/path", matcher.group("absPath"));
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
        {
            Matcher matcher = ABSOLUTE_URI_PATTERN.matcher("scheme://@@");
            matcher.matches();
            assertEquals("scheme://@@", matcher.group("absoluteUri"));
            assertEquals("//@@", matcher.group("hierPart"));
            assertEquals("//@@", matcher.group("netPath"));
            assertEquals("@@", matcher.group("authority"));
        }
        {
            Matcher matcher = ABSOLUTE_URI_PATTERN.matcher("scheme://user@C3A4:123/path?#?query#fragment");
            assertFalse(matcher.matches());
        }
        {
            Matcher matcher = ABSOLUTE_URI_PATTERN.matcher("urn:example:animal:ferret:nose");
            assertTrue(matcher.matches());
            assertEquals("urn:example:animal:ferret:nose", matcher.group("absoluteUri"));
            assertEquals("urn", matcher.group("scheme"));
            assertEquals("example:animal:ferret:nose", matcher.group("opaquePart"));
        }
        {
            Matcher matcher = ABSOLUTE_URI_PATTERN.matcher("urn:%2A-");
            assertTrue(matcher.matches());
            assertEquals("urn:%2A-", matcher.group("absoluteUri"));
            assertEquals("urn", matcher.group("scheme"));
            assertEquals("%2A-", matcher.group("opaquePart"));
        }
    }

    @Test
    void relativeUri() {
        {
            Matcher matcher = RELATIVE_URI_PATTERN.matcher("/path?query?query");
            assertTrue(matcher.matches());
            assertEquals("/path?query?query", matcher.group("relativeUri"));
            assertEquals("/path", matcher.group("absPath"));
            assertEquals("query?query", matcher.group("relativeUriQuery"));
            assertNull(matcher.group("authority"));
            assertNull(matcher.group("relPath"));
        }
        {
            Matcher matcher = RELATIVE_URI_PATTERN.matcher("//user@C3A4:123/path?query?query");
            assertTrue(matcher.matches());
            assertEquals("//user@C3A4:123/path?query?query", matcher.group("relativeUri"));
            assertEquals("user", matcher.group("userInfo"));
            assertEquals("123", matcher.group("port"));
            assertEquals("C3A4", matcher.group("host"));
            assertEquals("/path", matcher.group("absPath"));
            assertEquals("query?query", matcher.group("relativeUriQuery"));
            assertNull(matcher.group("relPath"));
        }
        {
            Matcher matcher = RELATIVE_URI_PATTERN.matcher("*");
            assertTrue(matcher.matches());
            assertEquals("*", matcher.group("relativeUri"));
            assertEquals("*", matcher.group("relPath"));
            assertNull(matcher.group("absPath"));
            assertNull(matcher.group("authority"));
        }
    }
}
