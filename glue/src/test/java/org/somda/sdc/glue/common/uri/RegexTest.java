package org.somda.sdc.glue.common.uri;

import org.junit.jupiter.api.Test;
import org.somda.sdc.glue.GlueConstants;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class RegexTest {

    private static final Pattern AUTHORITY_PATTERN = Pattern.compile("^" + GlueConstants.AUTHORITY + "$");
    private static final Pattern URI_PATTERN = Pattern.compile(GlueConstants.URI_REGEX);

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
            assertFalse(matcher.matches());
        }
    }
}
