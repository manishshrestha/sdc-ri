package org.somda.sdc.glue.common.helper;

import org.apache.http.client.utils.URLEncodedUtils;

import javax.annotation.Nullable;
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
     * @deprecated url encoding depends on the segment of the url, a generic "text-encoder" does therefore not make
     * sense. Currently only pchar according to rfc3986 is supported, see {@link #encodePChars(String)}.
     */
    @Deprecated(since = "2.1.0", forRemoval = true)
    public static String encode(@Nullable String text) {
        return encodePChars(text);
    }

    /**
     * Accepts a text and URL-decodes it based on UTF-8.
     *
     * @param text the text to decode.
     * @return the decoded text or an empty string if text was null.
     * @deprecated url encoding depends on the segment of the url, a generic "text-decoder" does therefore not make
     * sense. Currently only pchar according to rfc3986 is supported, see {@link #decodePChars(String)}.
     */
    @Deprecated(since = "2.1.0", forRemoval = true)
    public static String decode(@Nullable String text) {
        return decodePChars(text);
    }

    /**
     * Accepts a text and encodes it as valid pchar according to RFC3986.
     *
     * @param text the text to encode.
     * @return the encoded text or an empty string if text was null.
     */
    public static String encodePChars(@Nullable String text) {
        return encodePChars(text, false);
    }

    /**
     * Accepts a text and encodes it as valid pchar according to RFC3986.
     *
     * @param text the text to encode.
     * @param escapeAmpersand percent-encode ampersands (&amp;) in the text. This is required when the context already
     *                        uses ampersands as a delimiter, such as in the queries for
     *                        location context query transformation
     * @return the encoded text or an empty string if text was null.
     */
    public static String encodePChars(@Nullable String text, boolean escapeAmpersand) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        var encoded =  URLEncodedUtils.formatSegments(text).substring(1);
        if (escapeAmpersand) {
            encoded = encoded.replace("&", "%26");
        }
        return encoded;
    }

    /**
     * Accepts a text and decodes it as pchar according to RFC3986.
     *
     * @param text the text to decode.
     * @return the encoded text or an empty string if text was null.
     */
    public static String decodePChars(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        return URLEncodedUtils.parsePathSegments(text, StandardCharsets.UTF_8).get(0);
    }
}
