package org.somda.sdc.glue.common.uri;

import jregex.Matcher;
import jregex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.biceps.model.participant.SystemContextDescriptor;
import org.somda.sdc.glue.GlueConstants;
import test.org.somda.common.LoggingTestWatcher;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(LoggingTestWatcher.class)
public class RegexTest {

    private static final Pattern AUTHORITY_PATTERN = new Pattern("^" + GlueConstants.AUTHORITY + "$");
    private static final Pattern URI_PATTERN = new Pattern(GlueConstants.URI_REGEX);
    private static final Pattern URI_PATH = new Pattern(GlueConstants.RELATIVE_URI_REGEX);
    private static final Pattern SEGMENT_PATTERN = new Pattern(GlueConstants.AUTHORITY);

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
    void uriPath() {
        {
            Matcher matcher = URI_PATTERN.matcher("scheme://@@");
            matcher.matches();
            assertFalse(matcher.matches());
        }
        {
            Matcher matcher = URI_PATTERN.matcher("scheme://user@%C3%A4:123/path?#?query#fragment");
            assertFalse(matcher.matches());
        }
        {
            Matcher matcher = URI_PATH.matcher("scheme://user@%C3%A4:123/path?query?query#fragment");
            assertTrue(matcher.matches());
            assertEquals("/path", matcher.group("path"));
        }
        {
            Matcher matcher = URI_PATH.matcher("https://www.example.com");
            assertTrue(matcher.matches());
            assertEquals("", matcher.group("path"));
        }
        {
            Matcher matcher = URI_PATH.matcher("www.example.com");
            assertTrue(matcher.matches());
            assertEquals("", matcher.group("path"));
        }
        {
            Matcher matcher = URI_PATH.matcher("example.com");
            assertTrue(matcher.matches());
            assertEquals("", matcher.group("path"));
        }
        {
            Matcher matcher = URI_PATH.matcher("http://www.example.com/examples");
            assertTrue(matcher.matches());
            assertEquals("/examples", matcher.group("path"));
        }
        {
            Matcher matcher = URI_PATH.matcher("/examples");
            assertTrue(matcher.matches());
            assertEquals("/examples", matcher.group("path"));
        }
        {
            Matcher matcher = URI_PATH.matcher("/examples/first?id=1");
            assertTrue(matcher.matches());
            assertEquals("/examples/first", matcher.group("path"));
        }
        {
            Matcher matcher = URI_PATH.matcher("/examples/first?id=1#up");
            assertTrue(matcher.matches());
            assertEquals("/examples/first", matcher.group("path"));
        }
        {
            Matcher matcher = URI_PATH.matcher("http://www.example.com/examples?id=1&page=2");
            assertTrue(matcher.matches());
            assertEquals("/examples", matcher.group("path"));
        }
        {
            Matcher matcher = URI_PATH.matcher("http://www.example.com#up");
            assertTrue(matcher.matches());
            assertEquals("", matcher.group("path"));
        }
        {
            Matcher matcher = URI_PATH.matcher("http://www.example.com:8008");
            assertTrue(matcher.matches());
            assertEquals("", matcher.group("path"));
        }
        {
            Matcher matcher = URI_PATH.matcher("http://example.com/stuff.cgi?key= | http://bad-example.com/cgi-bin/stuff.cgi?key1=value1&key2");
            assertFalse(matcher.matches());
        }
    }

}
