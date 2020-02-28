package org.somda.sdc.glue.common.uri;

import jregex.Matcher;
import jregex.Pattern;
import org.junit.jupiter.api.Test;
import org.somda.sdc.glue.GlueConstants;

import static org.junit.jupiter.api.Assertions.*;

public class RegexTest {

    private static final Pattern AUTHORITY_PATTERN = new Pattern("^" + GlueConstants.AUTHORITY + "$");
    private static final Pattern URI_PATTERN = new Pattern(GlueConstants.URI_REGEX);

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
        {
            Matcher matcher = URI_PATTERN.matcher("scheme://@@");
            matcher.matches();
            System.out.println(matcher.group("path"));
            System.out.println(matcher.group("authority"));
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
    }
}
