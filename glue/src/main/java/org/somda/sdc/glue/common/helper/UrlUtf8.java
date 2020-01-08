package org.somda.sdc.glue.common.helper;

import javax.annotation.Nullable;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Helper to encode and decode URLs to and from UTF-8.
 */
public class UrlUtf8 {
    /**
     * Accepts a text and URL-encodes it based on UTF-8.
     *
     * @param text the text to encode.
     * @return the encoded text or an empty string if text was null.
     */
    public static String encode(@Nullable String text) {
        return text == null ? "" : URLEncoder.encode(text, StandardCharsets.UTF_8);
    }

    /**
     * Accepts a text and URL-decodes it based on UTF-8.
     *
     * @param text the text to decode.
     * @return the decoded text or an empty string if text was null.
     */
    public static String decode(@Nullable String text) {
        return text == null ? "" : URLDecoder.decode(text, StandardCharsets.UTF_8);
    }
}
